package com.baltito.crittervision

import android.graphics.ColorMatrix
import android.graphics.SurfaceTexture
import android.opengl.GLES11Ext
import android.opengl.GLES20
import android.util.Log
import android.util.Size
import android.view.Surface
import androidx.camera.core.SurfaceOutput
import androidx.camera.core.SurfaceProcessor
import androidx.camera.core.SurfaceRequest
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.util.concurrent.Executor
import javax.microedition.khronos.egl.EGL10
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.egl.EGLContext
import javax.microedition.khronos.egl.EGLDisplay
import javax.microedition.khronos.egl.EGLSurface

class ColorFilterSurfaceProcessor(private val glExecutor: Executor) : SurfaceProcessor {

    companion object {
        private const val TAG = "ColorFilterSurfaceProcessor"
        private const val VERTEX_SHADER_CODE =
            "attribute vec4 aPosition;\n" +
                    "attribute vec2 aTexCoord;\n" +
                    "varying vec2 vTexCoord;\n" +
                    "void main() {\n" +
                    "  gl_Position = aPosition;\n" +
                    "  vTexCoord = aTexCoord;\n" +
                    "}\n"

        private const val FRAGMENT_SHADER_CODE =
            "#extension GL_OES_EGL_image_external : require\n" +
                    "precision mediump float;\n" +
                    "varying vec2 vTexCoord;\n" +
                    "uniform samplerExternalOES sTexture;\n" +
                    "uniform mat4 uColorMatrix;\n" +
                    "uniform vec4 uColorOffset;\n" +
                    "void main() {\n" +
                    "  vec4 texColor = texture2D(sTexture, vTexCoord);\n" +
                    "  vec4 transformedColor = uColorMatrix * texColor + uColorOffset;\n" +
                    "  gl_FragColor = clamp(transformedColor, 0.0, 1.0);\n" +
                    "}\n"
    }

    private val egl: EGL10 = EGLContext.getEGL() as EGL10
    private var eglDisplay: EGLDisplay? = null
    private var eglContext: EGLContext? = null
    private var eglSurface: EGLSurface? = null

    private var programHandle: Int = 0
    private var positionHandle: Int = 0
    private var texCoordHandle: Int = 0
    private var colorMatrixHandle: Int = 0
    private var colorOffsetHandle: Int = 0
    private var oesTextureId: Int = 0

    private val vertexBuffer: FloatBuffer
    private val texCoordBuffer: FloatBuffer

    private var currentColorMatrix: ColorMatrix? = null // FIXED: Changed from ColorMatrixColorFilter to ColorMatrix
    private val glMatrix = FloatArray(16)
    private val glOffset = FloatArray(4)

    @Volatile private var isReleased = false

    // Resources for camera input
    private var surfaceTexture: SurfaceTexture? = null
    private var cameraSurface: Surface? = null

    // Output surface provided by CameraX
    private var surfaceOutput: SurfaceOutput? = null

    init {
        val vertices = floatArrayOf(
            -1.0f, -1.0f, 1.0f, -1.0f, -1.0f, 1.0f, 1.0f, 1.0f
        )
        val texCoords = floatArrayOf(
            0.0f, 0.0f, 1.0f, 0.0f, 0.0f, 1.0f, 1.0f, 1.0f
        )

        vertexBuffer = ByteBuffer.allocateDirect(vertices.size * 4).order(ByteOrder.nativeOrder()).asFloatBuffer()
        vertexBuffer.put(vertices).position(0)

        texCoordBuffer = ByteBuffer.allocateDirect(texCoords.size * 4).order(ByteOrder.nativeOrder()).asFloatBuffer()
        texCoordBuffer.put(texCoords).position(0)

        // Initialize with identity filter
        setFilter(null)
    }

    fun setFilter(colorMatrix: ColorMatrix?) { // FIXED: Changed parameter from ColorMatrixColorFilter to ColorMatrix
        currentColorMatrix = colorMatrix
        if (colorMatrix != null) {
            val matrixSrc = colorMatrix.array // FIXED: Direct access to array property
            glMatrix[0]=matrixSrc[0];  glMatrix[4]=matrixSrc[1];  glMatrix[8]=matrixSrc[2];   glMatrix[12]=matrixSrc[3];
            glMatrix[1]=matrixSrc[5];  glMatrix[5]=matrixSrc[6];  glMatrix[9]=matrixSrc[7];   glMatrix[13]=matrixSrc[8];
            glMatrix[2]=matrixSrc[10]; glMatrix[6]=matrixSrc[11]; glMatrix[10]=matrixSrc[12]; glMatrix[14]=matrixSrc[13];
            glMatrix[3]=matrixSrc[15]; glMatrix[7]=matrixSrc[16]; glMatrix[11]=matrixSrc[17]; glMatrix[15]=matrixSrc[18];
            glOffset[0] = matrixSrc[4] / 255.0f;
            glOffset[1] = matrixSrc[9] / 255.0f;
            glOffset[2] = matrixSrc[14] / 255.0f;
            glOffset[3] = matrixSrc[19] / 255.0f;
        } else {
            glMatrix.fill(0f); glOffset.fill(0f)
            glMatrix[0]=1f; glMatrix[5]=1f; glMatrix[10]=1f; glMatrix[15]=1f;
        }

        // If EGL context is initialized, apply the filter on GL thread
        if (eglDisplay != null && eglContext != null && eglSurface != null) {
            glExecutor.execute {
                if (isReleased) return@execute
                if (egl.eglMakeCurrent(eglDisplay, eglSurface, eglSurface, eglContext)) {
                    applyShaderUniforms()
                } else {
                    Log.e(TAG, "setFilter: eglMakeCurrent failed: ${egl.eglGetError()}")
                }
            }
        }
    }

    override fun onInputSurface(request: SurfaceRequest) {
        Log.d(TAG, "onInputSurface requested. Resolution: ${request.resolution}")
        if (isReleased) {
            Log.w(TAG, "onInputSurface: Processor already released. Will not provide surface.")
            request.willNotProvideSurface()
            return
        }

        glExecutor.execute {
            if (isReleased) {
                request.willNotProvideSurface()
                return@execute
            }
            try {
                initGLResources()

                surfaceTexture = SurfaceTexture(oesTextureId)
                surfaceTexture?.setDefaultBufferSize(request.resolution.width, request.resolution.height)
                cameraSurface = Surface(surfaceTexture)

                surfaceTexture?.setOnFrameAvailableListener({
                    if (isReleased) {
                        Log.w(TAG, "OnFrameAvailableListener: Processor released. Ignoring frame.")
                        return@setOnFrameAvailableListener
                    }
                    glExecutor.execute {
                        if (isReleased || eglDisplay == null || eglContext == null || eglSurface == null || surfaceOutput == null) {
                            Log.w(TAG, "OnFrameAvailableListener: GL context not ready or output missing. Skipping frame.")
                            return@execute
                        }
                        try {
                            if (egl.eglMakeCurrent(eglDisplay, eglSurface, eglSurface, eglContext)) {
                                surfaceTexture?.updateTexImage()
                                drawFrame(request.resolution)
                                egl.eglSwapBuffers(eglDisplay, eglSurface)
                            } else {
                                Log.e(TAG, "OnFrameAvailableListener: eglMakeCurrent failed: ${egl.eglGetError()}")
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "OnFrameAvailableListener: Error processing frame", e)
                        }
                    }
                }, null)

                Log.d(TAG, "Providing camera surface: $cameraSurface")
                request.provideSurface(cameraSurface!!, glExecutor) { result ->
                    Log.d(TAG, "Camera input surface released by CameraX. Result code: ${result.resultCode}")
                    glExecutor.execute {
                        if (isReleased) return@execute
                        surfaceTexture?.release()
                        cameraSurface?.release()
                        surfaceTexture = null
                        cameraSurface = null
                        Log.d(TAG, "Input surface released callback. Releasing processor.")
                        release()
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error in onInputSurface", e)
                request.willNotProvideSurface()
                release()
            }
        }
    }

    override fun onOutputSurface(surfaceOutput: SurfaceOutput) {
        Log.d(TAG, "onOutputSurface provided. SurfaceOutput: $surfaceOutput") // FIXED: Updated log message
        if (isReleased) {
            Log.w(TAG, "onOutputSurface: Processor already released.")
            return
        }

        glExecutor.execute {
            if (isReleased) return@execute
            try {
                this.surfaceOutput?.close()
                this.surfaceOutput = surfaceOutput

                val outputSurface = surfaceOutput.getSurface(glExecutor) { event -> // FIXED: Use getSurface correctly
                    Log.d(TAG, "SurfaceOutput event: ${event.eventCode}")
                    if (event.eventCode == SurfaceOutput.Event.EVENT_REQUEST_CLOSE) {
                        Log.w(TAG, "Output surface close requested by provider.")
                    }
                }

                initEGL(outputSurface) // FIXED: Pass the Surface directly
                setFilter(currentColorMatrix)

                Log.d(TAG, "EGL initialized for output surface.")
            } catch (e: Exception) {
                Log.e(TAG, "Error in onOutputSurface", e)
                release()
            }
        }
    }

    private fun initEGL(outputSurface: Surface) {
        eglDisplay = egl.eglGetDisplay(EGL10.EGL_DEFAULT_DISPLAY)
        if (eglDisplay == EGL10.EGL_NO_DISPLAY) throw RuntimeException("eglGetDisplay failed: ${egl.eglGetError()}")

        val version = IntArray(2)
        if (!egl.eglInitialize(eglDisplay, version)) throw RuntimeException("eglInitialize failed: ${egl.eglGetError()}")
        Log.d(TAG, "EGL Initialized. Version: ${version[0]}.${version[1]}")

        val attribList = intArrayOf(
            EGL10.EGL_RED_SIZE, 8, EGL10.EGL_GREEN_SIZE, 8, EGL10.EGL_BLUE_SIZE, 8,
            EGL10.EGL_ALPHA_SIZE, 8, EGL10.EGL_RENDERABLE_TYPE, 4,
            EGL10.EGL_SURFACE_TYPE, EGL10.EGL_WINDOW_BIT, EGL10.EGL_NONE
        )
        val numConfig = IntArray(1)
        val configs = arrayOfNulls<EGLConfig>(1)
        if (!egl.eglChooseConfig(eglDisplay, attribList, configs, 1, numConfig) || numConfig[0] == 0) {
            throw RuntimeException("eglChooseConfig failed: ${egl.eglGetError()}")
        }
        val eglConfig = configs[0] ?: throw RuntimeException("No EGLConfig found")

        val contextAttribs = intArrayOf(0x3098, 2, EGL10.EGL_NONE)
        eglContext = egl.eglCreateContext(eglDisplay, eglConfig, EGL10.EGL_NO_CONTEXT, contextAttribs)
        if (eglContext == EGL10.EGL_NO_CONTEXT) throw RuntimeException("eglCreateContext failed: ${egl.eglGetError()}")

        eglSurface = egl.eglCreateWindowSurface(eglDisplay, eglConfig, outputSurface, null)
        if (eglSurface == EGL10.EGL_NO_SURFACE) throw RuntimeException("eglCreateWindowSurface failed: ${egl.eglGetError()}")

        if (!egl.eglMakeCurrent(eglDisplay, eglSurface, eglSurface, eglContext)) {
            throw RuntimeException("eglMakeCurrent failed: ${egl.eglGetError()}")
        }
        Log.d(TAG, "EGL Context and Surface created and made current.")
    }

    private fun initGLResources() {
        if (oesTextureId == 0) {
            val textures = IntArray(1)
            GLES20.glGenTextures(1, textures, 0)
            oesTextureId = textures[0]
            GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, oesTextureId)
            GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_NEAREST)
            GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_NEAREST)
            GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE)
            GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE)
            GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, 0)
            Log.d(TAG, "OES Texture Initialized. Texture ID: $oesTextureId")
        }

        if (programHandle == 0) {
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
            Log.d(TAG, "GL Program Initialized. Program Handle: $programHandle")
        }
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

    private fun applyShaderUniforms() {
        GLES20.glUseProgram(programHandle)
        GLES20.glUniformMatrix4fv(colorMatrixHandle, 1, false, glMatrix, 0)
        GLES20.glUniform4fv(colorOffsetHandle, 1, glOffset, 0)
        GLES20.glUniform1i(GLES20.glGetUniformLocation(programHandle, "sTexture"), 0)
    }

    private fun drawFrame(resolution: Size) {
        if (isReleased) return

        GLES20.glViewport(0, 0, resolution.width, resolution.height)

        GLES20.glUseProgram(programHandle)
        applyShaderUniforms()

        GLES20.glEnableVertexAttribArray(positionHandle)
        GLES20.glVertexAttribPointer(positionHandle, 2, GLES20.GL_FLOAT, false, 0, vertexBuffer)

        GLES20.glEnableVertexAttribArray(texCoordHandle)
        GLES20.glVertexAttribPointer(texCoordHandle, 2, GLES20.GL_FLOAT, false, 0, texCoordBuffer)

        GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, oesTextureId)

        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4)

        GLES20.glDisableVertexAttribArray(positionHandle)
        GLES20.glDisableVertexAttribArray(texCoordHandle)
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, 0)
        GLES20.glUseProgram(0)
    }

    fun release() {
        if (isReleased) return
        isReleased = true
        Log.d(TAG, "Releasing ColorFilterSurfaceProcessor resources.")

        glExecutor.execute {
            surfaceOutput?.close()
            surfaceOutput = null

            surfaceTexture?.release()
            cameraSurface?.release()
            surfaceTexture = null
            cameraSurface = null

            if (eglDisplay != null) {
                egl.eglMakeCurrent(eglDisplay, EGL10.EGL_NO_SURFACE, EGL10.EGL_NO_SURFACE, EGL10.EGL_NO_CONTEXT)
                if (eglSurface != EGL10.EGL_NO_SURFACE && eglSurface != null) {
                    egl.eglDestroySurface(eglDisplay, eglSurface)
                    eglSurface = EGL10.EGL_NO_SURFACE
                }
                if (eglContext != EGL10.EGL_NO_CONTEXT && eglContext != null) {
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
            Log.d(TAG, "All EGL and GL resources released.")
        }
    }
}