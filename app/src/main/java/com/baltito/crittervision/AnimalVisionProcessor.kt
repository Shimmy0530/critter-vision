package com.baltito.crittervision

import android.graphics.SurfaceTexture
import android.opengl.EGL14
import android.opengl.EGLConfig
import android.opengl.EGLContext
import android.opengl.EGLDisplay
import android.opengl.EGLExt
import android.opengl.EGLSurface
import android.opengl.GLES11Ext
import android.opengl.GLES20
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import android.view.Surface
import androidx.camera.core.SurfaceOutput
import androidx.camera.core.SurfaceProcessor
import androidx.camera.core.SurfaceRequest
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.util.concurrent.Executor

/**
 * GPU-based camera frame processor using OpenGL ES 2.0 fragment shaders.
 *
 * Implements CameraX [SurfaceProcessor] to receive camera frames as OES textures,
 * apply a parameterized color matrix transformation (with sRGB linearization,
 * saturation boost, UV proxy, and intensity blending), and output to the preview surface.
 *
 * Frame processing takes <0.5ms on GPU — roughly 60× faster than 30fps requires.
 */
class AnimalVisionProcessor : SurfaceProcessor {

    companion object {
        private const val TAG = "AnimalVisionProcessor"

        private const val VERTEX_SHADER = """
            attribute vec4 aPosition;
            attribute vec2 aTexCoord;
            varying vec2 vTexCoord;
            uniform mat4 uTexMatrix;
            void main() {
                gl_Position = aPosition;
                vTexCoord = (uTexMatrix * vec4(aTexCoord, 0.0, 1.0)).xy;
            }
        """

        private const val FRAGMENT_SHADER = """
            #extension GL_OES_EGL_image_external : require
            precision mediump float;

            varying vec2 vTexCoord;
            uniform samplerExternalOES uTexture;
            uniform mat3 uColorMatrix;
            uniform vec3 uColorOffset;
            uniform float uSaturationBoost;
            uniform float uUvProxyWeight;
            uniform float uIntensity;

            vec3 srgbToLinear(vec3 c) {
                return mix(c / 12.92,
                           pow((c + 0.055) / 1.055, vec3(2.4)),
                           step(0.04045, c));
            }

            vec3 linearToSrgb(vec3 c) {
                c = clamp(c, 0.0, 1.0);
                return mix(c * 12.92,
                           1.055 * pow(c, vec3(1.0 / 2.4)) - 0.055,
                           step(0.0031308, c));
            }

            vec3 rgb2hsv(vec3 c) {
                vec4 K = vec4(0.0, -1.0 / 3.0, 2.0 / 3.0, -1.0);
                vec4 p = mix(vec4(c.bg, K.wz), vec4(c.gb, K.xy), step(c.b, c.g));
                vec4 q = mix(vec4(p.xyw, c.r), vec4(c.r, p.yzx), step(p.x, c.r));
                float d = q.x - min(q.w, q.y);
                float e = 1.0e-10;
                return vec3(abs(q.z + (q.w - q.y) / (6.0 * d + e)), d / (q.x + e), q.x);
            }

            vec3 hsv2rgb(vec3 c) {
                vec4 K = vec4(1.0, 2.0 / 3.0, 1.0 / 3.0, 3.0);
                vec3 p = abs(fract(c.xxx + K.xyz) * 6.0 - K.www);
                return c.z * mix(K.xxx, clamp(p - K.xxx, 0.0, 1.0), c.y);
            }

            void main() {
                vec4 texColor = texture2D(uTexture, vTexCoord);
                vec3 color = texColor.rgb;

                // Step 1: Linearize sRGB (dog/cat matrices expect linear input)
                vec3 linear = srgbToLinear(color);

                // Step 2: Apply 3x3 color matrix
                vec3 transformed = uColorMatrix * linear;
                transformed = clamp(transformed, 0.0, 1.0);

                // Step 3: Back to sRGB
                vec3 result = linearToSrgb(transformed);

                // Step 3.5: Additive color offset (display space, e.g. eagle brightness boost)
                result = clamp(result + uColorOffset, 0.0, 1.0);

                // Step 4: Saturation boost (bird mode — simulates oil droplet narrowing)
                if (uSaturationBoost > 1.001) {
                    vec3 hsv = rgb2hsv(result);
                    hsv.y = min(1.0, hsv.y * uSaturationBoost);
                    result = hsv2rgb(hsv);
                }

                // Step 5: UV proxy (bird mode — estimates UV-reflective surfaces)
                if (uUvProxyWeight > 0.001) {
                    float uvProxy = clamp(color.b - 0.5 * color.g - 0.3 * color.r, 0.0, 1.0);
                    uvProxy *= uUvProxyWeight;
                    result.b = clamp(result.b + uvProxy, 0.0, 1.0);
                    result.g = clamp(result.g - uvProxy * 0.2, 0.0, 1.0);
                }

                // Step 6: Intensity blend with original (slider control)
                result = mix(color, result, uIntensity);

                gl_FragColor = vec4(result, texColor.a);
            }
        """

        private val QUAD_VERTICES = floatArrayOf(
            -1f, -1f,
             1f, -1f,
            -1f,  1f,
             1f,  1f
        )

        private val QUAD_TEX_COORDS = floatArrayOf(
            0f, 0f,
            1f, 0f,
            0f, 1f,
            1f, 1f
        )
    }

    // GL thread
    private val glThread = HandlerThread("AnimalVisionGL").apply { start() }
    private val glHandler = Handler(glThread.looper)
    val glExecutor: Executor = Executor { command -> glHandler.post(command) }

    // EGL state
    private var eglDisplay: EGLDisplay = EGL14.EGL_NO_DISPLAY
    private var eglContext: EGLContext = EGL14.EGL_NO_CONTEXT
    private var eglConfig: EGLConfig? = null
    private var pbufferSurface: EGLSurface = EGL14.EGL_NO_SURFACE

    // Input (camera → SurfaceTexture → OES texture)
    private var inputTextureId = 0
    private var inputSurfaceTexture: SurfaceTexture? = null

    // Output (rendered frames → PreviewView)
    private var outputSurfaceOutput: SurfaceOutput? = null
    private var outputSurface: Surface? = null
    private var outputEglSurface: EGLSurface = EGL14.EGL_NO_SURFACE
    private var outputWidth = 0
    private var outputHeight = 0

    // Shader program and locations
    private var program = 0
    private var aPositionLoc = 0
    private var aTexCoordLoc = 0
    private var uTexMatrixLoc = 0
    private var uTextureLoc = 0
    private var uColorMatrixLoc = 0
    private var uColorOffsetLoc = 0
    private var uSatBoostLoc = 0
    private var uUvProxyLoc = 0
    private var uIntensityLoc = 0

    // Geometry buffers
    private val vertexBuffer: FloatBuffer = createFloatBuffer(QUAD_VERTICES)
    private val texCoordBuffer: FloatBuffer = createFloatBuffer(QUAD_TEX_COORDS)
    private val texMatrix = FloatArray(16)

    // Vision parameters — volatile for thread-safe reads from GL thread
    @Volatile var colorMatrix = floatArrayOf(1f, 0f, 0f, 0f, 1f, 0f, 0f, 0f, 1f)
    @Volatile var colorOffset = floatArrayOf(0f, 0f, 0f)
    @Volatile var saturationBoost = 1.0f
    @Volatile var uvProxyWeight = 0.0f
    @Volatile var intensity = 1.0f

    private var isGlInitialized = false

    /**
     * Update all vision parameters at once from a [VisionColorFilter.VisionParams].
     */
    fun setVisionParams(params: VisionColorFilter.VisionParams, intensity: Float = 1.0f) {
        this.colorMatrix = params.matrix.copyOf()
        this.colorOffset = params.colorOffset.copyOf()
        this.saturationBoost = params.saturationBoost
        this.uvProxyWeight = params.uvProxyWeight
        this.intensity = intensity
    }

    // ── SurfaceProcessor callbacks ──────────────────────────────────────

    override fun onInputSurface(request: SurfaceRequest) {
        val size = request.resolution
        Log.d(TAG, "onInputSurface: ${size.width}x${size.height}")

        glHandler.post {
            ensureGlInitialized()

            // Create OES texture for camera input
            val texIds = IntArray(1)
            GLES20.glGenTextures(1, texIds, 0)
            inputTextureId = texIds[0]
            GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, inputTextureId)
            GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR)
            GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR)
            GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE)
            GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE)

            val surfaceTexture = SurfaceTexture(inputTextureId)
            surfaceTexture.setDefaultBufferSize(size.width, size.height)
            surfaceTexture.setOnFrameAvailableListener({ renderFrame() }, glHandler)
            inputSurfaceTexture = surfaceTexture

            val surface = Surface(surfaceTexture)
            request.provideSurface(surface, glExecutor) { result ->
                Log.d(TAG, "Input surface released: code=${result.resultCode}")
                surface.release()
                surfaceTexture.release()
                inputSurfaceTexture = null
            }
        }
    }

    override fun onOutputSurface(surfaceOutput: SurfaceOutput) {
        val size = surfaceOutput.size
        Log.d(TAG, "onOutputSurface: ${size.width}x${size.height}")

        glHandler.post {
            ensureGlInitialized()
            releaseOutputSurface()

            outputSurfaceOutput = surfaceOutput
            outputSurface = surfaceOutput.getSurface(glExecutor) { _ ->
                Log.d(TAG, "Output surface event: requestClose")
                glHandler.post {
                    releaseOutputSurface()
                    surfaceOutput.close()
                }
            }
            outputWidth = size.width
            outputHeight = size.height

            val surfaceAttribs = intArrayOf(EGL14.EGL_NONE)
            outputEglSurface = EGL14.eglCreateWindowSurface(
                eglDisplay, eglConfig, outputSurface, surfaceAttribs, 0
            )
            checkEglError("eglCreateWindowSurface")
            Log.d(TAG, "Output EGL surface created: ${outputWidth}x${outputHeight}")
        }
    }

    // ── Rendering ───────────────────────────────────────────────────────

    private fun renderFrame() {
        val st = inputSurfaceTexture ?: return
        if (outputEglSurface == EGL14.EGL_NO_SURFACE) return

        // Latch the latest camera frame
        st.updateTexImage()
        st.getTransformMatrix(texMatrix)

        // Make output surface current
        EGL14.eglMakeCurrent(eglDisplay, outputEglSurface, outputEglSurface, eglContext)

        GLES20.glViewport(0, 0, outputWidth, outputHeight)
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)
        GLES20.glUseProgram(program)

        // Bind camera texture
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, inputTextureId)
        GLES20.glUniform1i(uTextureLoc, 0)

        // Texture transform (handles camera rotation/flip)
        GLES20.glUniformMatrix4fv(uTexMatrixLoc, 1, false, texMatrix, 0)

        // Color matrix — transpose from row-major (science) to column-major (GLSL)
        val cm = colorMatrix
        val glMatrix = floatArrayOf(
            cm[0], cm[3], cm[6],
            cm[1], cm[4], cm[7],
            cm[2], cm[5], cm[8]
        )
        GLES20.glUniformMatrix3fv(uColorMatrixLoc, 1, false, glMatrix, 0)

        // Color offset (display-space additive)
        val co = colorOffset
        GLES20.glUniform3fv(uColorOffsetLoc, 1, co, 0)

        // Vision parameters
        GLES20.glUniform1f(uSatBoostLoc, saturationBoost)
        GLES20.glUniform1f(uUvProxyLoc, uvProxyWeight)
        GLES20.glUniform1f(uIntensityLoc, intensity)

        // Draw fullscreen quad
        vertexBuffer.position(0)
        GLES20.glVertexAttribPointer(aPositionLoc, 2, GLES20.GL_FLOAT, false, 0, vertexBuffer)
        GLES20.glEnableVertexAttribArray(aPositionLoc)

        texCoordBuffer.position(0)
        GLES20.glVertexAttribPointer(aTexCoordLoc, 2, GLES20.GL_FLOAT, false, 0, texCoordBuffer)
        GLES20.glEnableVertexAttribArray(aTexCoordLoc)

        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4)

        EGL14.eglSwapBuffers(eglDisplay, outputEglSurface)
    }

    // ── EGL + Shader initialization ─────────────────────────────────────

    private fun ensureGlInitialized() {
        if (isGlInitialized) return
        initEgl()
        initShaders()
        isGlInitialized = true
    }

    private fun initEgl() {
        eglDisplay = EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY)
        if (eglDisplay == EGL14.EGL_NO_DISPLAY) throw RuntimeException("eglGetDisplay failed")

        val version = IntArray(2)
        if (!EGL14.eglInitialize(eglDisplay, version, 0, version, 1)) {
            throw RuntimeException("eglInitialize failed")
        }

        val configAttribs = intArrayOf(
            EGL14.EGL_RED_SIZE, 8,
            EGL14.EGL_GREEN_SIZE, 8,
            EGL14.EGL_BLUE_SIZE, 8,
            EGL14.EGL_ALPHA_SIZE, 8,
            EGL14.EGL_RENDERABLE_TYPE, EGL14.EGL_OPENGL_ES2_BIT,
            EGL14.EGL_SURFACE_TYPE, EGL14.EGL_WINDOW_BIT or EGL14.EGL_PBUFFER_BIT,
            EGLExt.EGL_RECORDABLE_ANDROID, 1,
            EGL14.EGL_NONE
        )

        val configs = arrayOfNulls<EGLConfig>(1)
        val numConfigs = IntArray(1)
        EGL14.eglChooseConfig(eglDisplay, configAttribs, 0, configs, 0, 1, numConfigs, 0)
        if (numConfigs[0] == 0) throw RuntimeException("No suitable EGL config")
        eglConfig = configs[0]!!

        val contextAttribs = intArrayOf(
            EGL14.EGL_CONTEXT_CLIENT_VERSION, 2,
            EGL14.EGL_NONE
        )
        eglContext = EGL14.eglCreateContext(
            eglDisplay, eglConfig, EGL14.EGL_NO_CONTEXT, contextAttribs, 0
        )
        checkEglError("eglCreateContext")

        // Tiny pbuffer to make context current for shader compilation
        val pbufferAttribs = intArrayOf(
            EGL14.EGL_WIDTH, 1,
            EGL14.EGL_HEIGHT, 1,
            EGL14.EGL_NONE
        )
        pbufferSurface = EGL14.eglCreatePbufferSurface(eglDisplay, eglConfig, pbufferAttribs, 0)
        EGL14.eglMakeCurrent(eglDisplay, pbufferSurface, pbufferSurface, eglContext)
        checkEglError("eglMakeCurrent (init)")

        Log.d(TAG, "EGL initialized: version ${version[0]}.${version[1]}")
    }

    private fun initShaders() {
        val vertexShader = compileShader(GLES20.GL_VERTEX_SHADER, VERTEX_SHADER)
        val fragmentShader = compileShader(GLES20.GL_FRAGMENT_SHADER, FRAGMENT_SHADER)

        program = GLES20.glCreateProgram()
        GLES20.glAttachShader(program, vertexShader)
        GLES20.glAttachShader(program, fragmentShader)
        GLES20.glLinkProgram(program)

        val linkStatus = IntArray(1)
        GLES20.glGetProgramiv(program, GLES20.GL_LINK_STATUS, linkStatus, 0)
        if (linkStatus[0] != GLES20.GL_TRUE) {
            val log = GLES20.glGetProgramInfoLog(program)
            GLES20.glDeleteProgram(program)
            throw RuntimeException("Program link failed: $log")
        }

        // Clean up individual shaders (now linked into program)
        GLES20.glDeleteShader(vertexShader)
        GLES20.glDeleteShader(fragmentShader)

        aPositionLoc = GLES20.glGetAttribLocation(program, "aPosition")
        aTexCoordLoc = GLES20.glGetAttribLocation(program, "aTexCoord")
        uTexMatrixLoc = GLES20.glGetUniformLocation(program, "uTexMatrix")
        uTextureLoc = GLES20.glGetUniformLocation(program, "uTexture")
        uColorMatrixLoc = GLES20.glGetUniformLocation(program, "uColorMatrix")
        uColorOffsetLoc = GLES20.glGetUniformLocation(program, "uColorOffset")
        uSatBoostLoc = GLES20.glGetUniformLocation(program, "uSaturationBoost")
        uUvProxyLoc = GLES20.glGetUniformLocation(program, "uUvProxyWeight")
        uIntensityLoc = GLES20.glGetUniformLocation(program, "uIntensity")

        Log.d(TAG, "Shaders compiled and linked")
    }

    private fun compileShader(type: Int, source: String): Int {
        val shader = GLES20.glCreateShader(type)
        GLES20.glShaderSource(shader, source)
        GLES20.glCompileShader(shader)

        val compiled = IntArray(1)
        GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, compiled, 0)
        if (compiled[0] != GLES20.GL_TRUE) {
            val log = GLES20.glGetShaderInfoLog(shader)
            GLES20.glDeleteShader(shader)
            throw RuntimeException("Shader compile failed (${if (type == GLES20.GL_VERTEX_SHADER) "vertex" else "fragment"}): $log")
        }
        return shader
    }

    // ── Cleanup ─────────────────────────────────────────────────────────

    private fun releaseOutputSurface() {
        if (outputEglSurface != EGL14.EGL_NO_SURFACE) {
            EGL14.eglDestroySurface(eglDisplay, outputEglSurface)
            outputEglSurface = EGL14.EGL_NO_SURFACE
        }
        outputSurface = null
        outputSurfaceOutput = null
    }

    fun release() {
        glHandler.post {
            if (program != 0) {
                GLES20.glDeleteProgram(program)
                program = 0
            }
            if (inputTextureId != 0) {
                GLES20.glDeleteTextures(1, intArrayOf(inputTextureId), 0)
                inputTextureId = 0
            }
            releaseOutputSurface()
            if (pbufferSurface != EGL14.EGL_NO_SURFACE) {
                EGL14.eglDestroySurface(eglDisplay, pbufferSurface)
                pbufferSurface = EGL14.EGL_NO_SURFACE
            }
            if (eglContext != EGL14.EGL_NO_CONTEXT) {
                EGL14.eglDestroyContext(eglDisplay, eglContext)
                eglContext = EGL14.EGL_NO_CONTEXT
            }
            if (eglDisplay != EGL14.EGL_NO_DISPLAY) {
                EGL14.eglTerminate(eglDisplay)
                eglDisplay = EGL14.EGL_NO_DISPLAY
            }
            isGlInitialized = false
            glThread.quitSafely()
        }
    }

    // ── Utilities ───────────────────────────────────────────────────────

    private fun createFloatBuffer(data: FloatArray): FloatBuffer {
        return ByteBuffer.allocateDirect(data.size * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()
            .apply { put(data); position(0) }
    }

    private fun checkEglError(op: String) {
        val error = EGL14.eglGetError()
        if (error != EGL14.EGL_SUCCESS) {
            Log.e(TAG, "$op: EGL error 0x${Integer.toHexString(error)}")
        }
    }
}
