package com.example.crittervision

import android.graphics.ColorMatrixColorFilter
import android.opengl.GLES11Ext
import android.opengl.GLES20
import android.graphics.SurfaceTexture // Needed for proper camera texture handling
import android.util.Log
import android.util.Size
import android.view.Surface
import androidx.camera.core.SurfaceOutput
import androidx.camera.core.SurfaceProcessor
import androidx.camera.core.SurfaceRequest
import java.util.concurrent.Executor
import javax.microedition.khronos.egl.EGL10
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.egl.EGLContext
import javax.microedition.khronos.egl.EGLDisplay
import javax.microedition.khronos.egl.EGLSurface
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer

class ColorFilterSurfaceProcessor(private val glExecutor: Executor) : SurfaceProcessor {

    companion object {
        private const val TAG = "ColorFilterSurfaceProcessor"
        // Basic pass-through vertex shader
        private const val VERTEX_SHADER_CODE =
            "attribute vec4 aPosition;\n" +
            "attribute vec2 aTexCoord;\n" +
            "varying vec2 vTexCoord;\n" +
            "void main() {\n" +
            "  gl_Position = aPosition;\n" +
            "  vTexCoord = aTexCoord;\n" +
            "}\n"

        // Fragment shader to apply a color matrix
        // Uses an OES texture sampler
        private const val FRAGMENT_SHADER_CODE =
            "#extension GL_OES_EGL_image_external : require\n" +
            "precision mediump float;\n" +
            "varying vec2 vTexCoord;\n" +
            "uniform samplerExternalOES sTexture;\n" + // OES texture sampler
            "uniform mat4 uColorMatrix;\n" +
            "uniform vec4 uColorOffset;\n" +
            "void main() {\n" +
            "  vec4 texColor = texture2D(sTexture, vTexCoord);\n" +
            "  vec4 transformedColor = uColorMatrix * texColor + uColorOffset;\n" +
            "  gl_FragColor = clamp(transformedColor, 0.0, 1.0);\n" +
            "}\n"
    }

    private var eglDisplay: EGLDisplay? = null
    private var eglContext: EGLContext? = null
    private var eglSurface: EGLSurface? = null

    private var programHandle: Int = 0
    private var positionHandle: Int = 0
    private var texCoordHandle: Int = 0
    private var colorMatrixHandle: Int = 0
    private var colorOffsetHandle: Int = 0
    private var oesTextureId: Int = 0 // Input OES texture ID from SurfaceTexture

    private val vertexBuffer: FloatBuffer
    private val texCoordBuffer: FloatBuffer

    private var currentFilter: ColorMatrixColorFilter? = null
    private val glMatrix = FloatArray(16) // For 4x4 part of ColorMatrix
    private val glOffset = FloatArray(4)  // For offset part of ColorMatrix (normalized)

    private var surfaceTexture: SurfaceTexture? = null
    private var cameraSurface: Surface? = null
    private var surfaceOutput: SurfaceOutput? = null
    @Volatile private var isReleased = false


    init {
        // Quad vertices: covers the entire viewport in normalized device coordinates
        val vertices = floatArrayOf(
            -1.0f, -1.0f,  // Bottom Left
             1.0f, -1.0f,  // Bottom Right
            -1.0f,  1.0f,  // Top Left
             1.0f,  1.0f   // Top Right
        )
        // Texture coordinates: maps texture to the quad
        val texCoords = floatArrayOf(
            0.0f, 0.0f,  // Bottom Left
            1.0f, 0.0f,  // Bottom Right
            0.0f, 1.0f,  // Top Left
            1.0f, 1.0f   // Top Right
        )

        vertexBuffer = ByteBuffer.allocateDirect(vertices.size * 4).order(ByteOrder.nativeOrder()).asFloatBuffer()
        vertexBuffer.put(vertices).position(0)

        texCoordBuffer = ByteBuffer.allocateDirect(texCoords.size * 4).order(ByteOrder.nativeOrder()).asFloatBuffer()
        texCoordBuffer.put(texCoords).position(0)
    }

    fun setFilter(filter: ColorMatrixColorFilter?) {
        // This method can be called from any thread.
        // The actual GL uniform update should happen on the GL thread.
        // We store the filter and convert matrix components here.
        currentFilter = filter
        if (filter != null) {
            val matrixSrc = filter.colorMatrix?.array ?: floatArrayOf(
                1f,0f,0f,0f,0f, 0f,1f,0f,0f,0f, 0f,0f,1f,0f,0f, 0f,0f,0f,1f,0f)

            // Transpose the 4x4 part of the Android ColorMatrix (row-major)
            // into a column-major matrix for OpenGL's uColorMatrix.
            glMatrix[0]=matrixSrc[0];  glMatrix[4]=matrixSrc[1];  glMatrix[8]=matrixSrc[2];   glMatrix[12]=matrixSrc[3];
            glMatrix[1]=matrixSrc[5];  glMatrix[5]=matrixSrc[6];  glMatrix[9]=matrixSrc[7];   glMatrix[13]=matrixSrc[8];
            glMatrix[2]=matrixSrc[10]; glMatrix[6]=matrixSrc[11]; glMatrix[10]=matrixSrc[12]; glMatrix[14]=matrixSrc[13];
            glMatrix[3]=matrixSrc[15]; glMatrix[7]=matrixSrc[16]; glMatrix[11]=matrixSrc[17]; glMatrix[15]=matrixSrc[18];

            // Normalize the offset vector (5th column of Android ColorMatrix)
            // Android offsets are typically 0-255, shader expects 0-1.
            glOffset[0] = matrixSrc[4] / 255.0f;
            glOffset[1] = matrixSrc[9] / 255.0f;
            glOffset[2] = matrixSrc[14] / 255.0f;
            glOffset[3] = matrixSrc[19] / 255.0f; // Alpha offset

        } else { // Identity matrix and zero offset for no filter
            glMatrix.fill(0f); glOffset.fill(0f)
            glMatrix[0]=1f; glMatrix[5]=1f; glMatrix[10]=1f; glMatrix[15]=1f; // Diagonal for identity
        }
        // Trigger a redraw if GL context is initialized (not shown here, but good practice)
    }


    override fun onSurfaceRequested(request: SurfaceRequest) {
        if (isReleased) {
            request.willNotProvideSurface()
            return
        }
        glExecutor.execute {
            try {
                val targetSurface = request.surface
                val resolution = request.resolution
                Log.d(TAG, "onSurfaceRequested: Resolution ${resolution.width}x${resolution.height}, Surface: $targetSurface")

                initEGL(targetSurface)
                initGLResources() // Initialize GL shaders, program, and OES texture

                // Create SurfaceTexture and Surface for CameraX output.
                surfaceTexture = SurfaceTexture(oesTextureId)
                surfaceTexture?.setDefaultBufferSize(resolution.width, resolution.height)
                cameraSurface = Surface(surfaceTexture)

                surfaceTexture?.setOnFrameAvailableListener {
                    if (isReleased) return@setOnFrameAvailableListener
                    glExecutor.execute { // Ensure GL operations on GL thread
                        if (isReleased || eglDisplay == null || eglSurface == null || eglContext == null) return@execute
                        try {
                            if (egl.eglMakeCurrent(eglDisplay, eglSurface, eglSurface, eglContext)) {
                                surfaceTexture?.updateTexImage()
                                drawFrame(resolution)
                                egl.eglSwapBuffers(eglDisplay, eglSurface)
                            } else {
                                Log.e(TAG, "eglMakeCurrent failed in frame loop: ${egl.eglGetError()}")
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "Error in frame processing loop", e)
                        }
                    }
                }

                this.surfaceOutput = request.deferrableSurface.getSurfaceOutput()
                this.surfaceOutput?.setSurface(cameraSurface!!) // CameraX writes to this surface

                Log.d(TAG, "Surface request processed. CameraX outputting to our SurfaceTexture.")

            } catch (e: Exception) {
                Log.e(TAG, "Error processing surface request", e)
                request.willNotProvideSurface() // Inform CameraX about the failure
                release() // Release resources on error
            }
        }
    }

    private val egl: EGL10 = EGLContext.getEGL() as EGL10

    private fun initEGL(surface: Surface) {
        eglDisplay = egl.eglGetDisplay(EGL10.EGL_DEFAULT_DISPLAY)
        if (eglDisplay == EGL10.EGL_NO_DISPLAY) throw RuntimeException("eglGetDisplay failed: ${egl.eglGetError()}")

        val version = IntArray(2)
        if (!egl.eglInitialize(eglDisplay, version)) throw RuntimeException("eglInitialize failed: ${egl.eglGetError()}")
        Log.d(TAG, "EGL Initialized. Version: ${version[0]}.${version[1]}")

        val attribList = intArrayOf(
            EGL10.EGL_RED_SIZE, 8, EGL10.EGL_GREEN_SIZE, 8, EGL10.EGL_BLUE_SIZE, 8,
            EGL10.EGL_ALPHA_SIZE, 8, EGL10.EGL_RENDERABLE_TYPE, 4, // EGL_OPENGL_ES2_BIT
            EGL10.EGL_SURFACE_TYPE, EGL10.EGL_WINDOW_BIT, EGL10.EGL_NONE
        )
        val numConfig = IntArray(1)
        val configs = arrayOfNulls<EGLConfig>(1)
        if (!egl.eglChooseConfig(eglDisplay, attribList, configs, 1, numConfig) || numConfig[0] == 0) {
            throw RuntimeException("eglChooseConfig failed or no matching config found: ${egl.eglGetError()}")
        }
        val eglConfig = configs[0] ?: throw RuntimeException("No EGLConfig found")

        val contextAttribs = intArrayOf(0x3098 /*EGL_CONTEXT_CLIENT_VERSION*/, 2, EGL10.EGL_NONE)
        eglContext = egl.eglCreateContext(eglDisplay, eglConfig, EGL10.EGL_NO_CONTEXT, contextAttribs)
        if (eglContext == EGL10.EGL_NO_CONTEXT) throw RuntimeException("eglCreateContext failed: ${egl.eglGetError()}")

        eglSurface = egl.eglCreateWindowSurface(eglDisplay, eglConfig, surface, null)
        if (eglSurface == EGL10.EGL_NO_SURFACE) throw RuntimeException("eglCreateWindowSurface failed: ${egl.eglGetError()}")

        if (!egl.eglMakeCurrent(eglDisplay, eglSurface, eglSurface, eglContext)) {
            throw RuntimeException("eglMakeCurrent failed: ${egl.eglGetError()}")
        }
        setFilter(currentFilter) // Apply current filter (or identity)
    }

    private fun initGLResources() {
        programHandle = GLES20.glCreateProgram().also {
            GLES20.glAttachShader(it, loadShader(GLES20.GL_VERTEX_SHADER, VERTEX_SHADER_CODE))
            GLES20.glAttachShader(it, loadShader(GLES20.GL_FRAGMENT_SHADER, FRAGMENT_SHADER_CODE))
            GLES20.glLinkProgram(it)
            val linkStatus = IntArray(1)
            GLES20.glGetProgramiv(it, GLES20.GL_LINK_STATUS, linkStatus, 0)
            if (linkStatus[0] == 0) {
                val info = GLES20.glGetProgramInfoLog(it)
                GLES20.glDeleteProgram(it)
                throw RuntimeException("Could not link program: $info")
            }
        }
        positionHandle = GLES20.glGetAttribLocation(programHandle, "aPosition")
        texCoordHandle = GLES20.glGetAttribLocation(programHandle, "aTexCoord")
        colorMatrixHandle = GLES20.glGetUniformLocation(programHandle, "uColorMatrix")
        colorOffsetHandle = GLES20.glGetUniformLocation(programHandle, "uColorOffset")

        // Create OES texture for camera input
        val textures = IntArray(1)
        GLES20.glGenTextures(1, textures, 0)
        oesTextureId = textures[0]
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, oesTextureId)
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_NEAREST)
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_NEAREST)
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE)
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE)
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, 0) // Unbind
        Log.d(TAG, "GL Program and OES Texture Initialized. Program: $programHandle, Texture: $oesTextureId")
    }

    private fun loadShader(type: Int, shaderCode: String): Int {
        return GLES20.glCreateShader(type).also { shader ->
            GLES20.glShaderSource(shader, shaderCode)
            GLES20.glCompileShader(shader)
            val compiled = IntArray(1)
            GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, compiled, 0)
            if (compiled[0] == 0) {
                val info = GLES20.glGetShaderInfoLog(shader)
                GLES20.glDeleteShader(shader)
                throw RuntimeException("Could not compile shader $type: $info")
            }
        }
    }

    private fun drawFrame(resolution: Size) {
        GLES20.glViewport(0, 0, resolution.width, resolution.height)
        GLES20.glClearColor(0.0f, 0.0f, 0.0f, 1.0f)
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)

        GLES20.glUseProgram(programHandle)

        GLES20.glEnableVertexAttribArray(positionHandle)
        GLES20.glVertexAttribPointer(positionHandle, 2, GLES20.GL_FLOAT, false, 0, vertexBuffer)

        GLES20.glEnableVertexAttribArray(texCoordHandle)
        GLES20.glVertexAttribPointer(texCoordHandle, 2, GLES20.GL_FLOAT, false, 0, texCoordBuffer)

        GLES20.glUniformMatrix4fv(colorMatrixHandle, 1, false, glMatrix, 0)
        GLES20.glUniform4fv(colorOffsetHandle, 1, glOffset, 0)

        GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, oesTextureId)
        GLES20.glUniform1i(GLES20.glGetUniformLocation(programHandle, "sTexture"), 0)

        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4)

        GLES20.glDisableVertexAttribArray(positionHandle)
        GLES20.glDisableVertexAttribArray(texCoordHandle)
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, 0) // Unbind texture
        GLES20.glUseProgram(0)
    }

    override fun onSurfaceOutputCloseRequested(surfaceOutput: SurfaceOutput) {
        Log.d(TAG, "onSurfaceOutputCloseRequested called on thread: ${Thread.currentThread().name}")
        glExecutor.execute {
            release()
        }
    }

    private fun release() {
        if (isReleased) return
        isReleased = true
        Log.d(TAG, "Releasing EGL and GL resources.")
        surfaceTexture?.release()
        cameraSurface?.release()
        surfaceTexture = null
        cameraSurface = null

        if (eglDisplay != null) {
            egl.eglMakeCurrent(eglDisplay, EGL10.EGL_NO_SURFACE, EGL10.EGL_NO_SURFACE, EGL10.EGL_NO_CONTEXT)
            if (eglSurface != EGL10.EGL_NO_SURFACE) {
                egl.eglDestroySurface(eglDisplay, eglSurface)
                eglSurface = EGL10.EGL_NO_SURFACE
            }
            if (eglContext != EGL10.EGL_NO_CONTEXT) {
                egl.eglDestroyContext(eglDisplay, eglContext)
                eglContext = EGL10.EGL_NO_CONTEXT
            }
            egl.eglTerminate(eglDisplay)
            eglDisplay = EGL10.EGL_NO_DISPLAY
        }
        if (programHandle != 0) {
            GLES20.glDeleteProgram(programHandle)
            programHandle = 0
        }
        if (oesTextureId != 0) {
            GLES20.glDeleteTextures(1, intArrayOf(oesTextureId), 0)
            oesTextureId = 0
        }
        Log.d(TAG, "EGL and GL resources released.")
    }
}
