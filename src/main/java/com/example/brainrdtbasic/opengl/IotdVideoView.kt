package com.example.brainrdtbasic.opengl

import android.content.Context
import android.graphics.SurfaceTexture
import android.media.MediaPlayer
import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.opengl.Matrix
import android.util.Log
import android.view.Surface
import com.example.brainrdtbasic.mmToPx
import com.example.brainrdtbasic.pxToMm
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

class IotdVideoView(context: Context, val mediaPlayer: MediaPlayer): GLSurfaceView(context),
    IStereoVideoView {
    private val videoRenderer by lazy { VideoRenderer(context, mediaPlayer) }

    init {
        setEGLContextClientVersion(2)
        setRenderer(videoRenderer)
    }

    override fun onResume() {
        super.onResume()
    }

    override fun setVideoAttributes(videoWidthMm: Float, videoDistanceMm: Float) {
        videoRenderer.setVideoAttributes(videoWidthMm, videoDistanceMm)
    }

    override fun setIotdValue(iotdValue: Int) {
        // Do nothing
    }

    class VideoRenderer(val context: Context, val mMediaPlayer: MediaPlayer) : GLSurfaceView.Renderer, SurfaceTexture.OnFrameAvailableListener {
        private val mVertexShader = """uniform mat4 uMVPMatrix;
                                        uniform mat4 uSTMatrix;
                                        attribute vec4 aPosition;
                                        attribute vec4 aTextureCoord;
                                        varying vec2 vTextureCoord;
                                        void main() {
                                          gl_Position = uMVPMatrix * aPosition;
                                          vTextureCoord = (uSTMatrix * aTextureCoord).xy;
                                        }
                                        """

        private val mFragmentShader = """#extension GL_OES_EGL_image_external : require
                                        precision mediump float;
                                        varying vec2 vTextureCoord;
                                        uniform samplerExternalOES sTexture;
                                        void main() {
                                          gl_FragColor = texture2D(sTexture, vTextureCoord);
                                        }
                                        """
        private val TAG = "VideoRender"

        private val FLOAT_SIZE_BYTES = 4
        private val TRIANGLE_VERTICES_DATA_STRIDE_BYTES = 5 * FLOAT_SIZE_BYTES
        private val TRIANGLE_VERTICES_DATA_POS_OFFSET = 0
        private val TRIANGLE_VERTICES_DATA_UV_OFFSET = 3
        private val mTriangleVerticesData = floatArrayOf(
            // X, Y, Z, U, V
            -1.0f, -1.0f, 0f, 0f, 0f,
            1.0f, -1.0f, 0f, 0.5f, 0f,
            -1.0f, 1.0f, 0f, 0f, 1f,
            1.0f, 1.0f, 0f, 0.5f, 1f,
        )
        private val mTriangleVerticesDataRight = floatArrayOf(
            // X, Y, Z, U, V
            -1.0f, -1.0f, 0f, 0.5f, 0f,
            1.0f, -1.0f, 0f, 1f, 0f,
            -1.0f, 1.0f, 0f, 0.5f, 1f,
            1.0f, 1.0f, 0f, 1f, 1f,
        )

        private var mTriangleVertices: FloatBuffer? = null
        private var mTriangleVerticesRight: FloatBuffer? = null

        private var mMVPMatrix = FloatArray(16)
        private var mMVPMatrixRight = FloatArray(16)
        private var mSTMatrix = FloatArray(16)

        private var mProgram = 0
        private var mTextureID = 0
        private var muMVPMatrixHandle = 0
        private var muSTMatrixHandle = 0
        private var maPositionHandle = 0
        private var maTextureHandle = 0

        private var mSurface: SurfaceTexture? = null
        private var updateSurface = false

        private var GL_TEXTURE_EXTERNAL_OES = 0x8D65

        private var viewWidth = 1.0f
        private var viewHeight = 1.0f
        private var videoWidth = 1.0f
        private var videoHeight = 1.0f

        private var videoWidthMm = 1.0f
        private var videoDistanceMm = 1.0f

        init {
            mTriangleVertices = ByteBuffer.allocateDirect(
                mTriangleVerticesData.count() * FLOAT_SIZE_BYTES)
                .order(ByteOrder.nativeOrder()).asFloatBuffer();
            mTriangleVertices?.put(mTriangleVerticesData)?.position(0)

            mTriangleVerticesRight = ByteBuffer.allocateDirect(
                mTriangleVerticesDataRight.count() * FLOAT_SIZE_BYTES)
                .order(ByteOrder.nativeOrder()).asFloatBuffer()
            mTriangleVerticesRight?.put(mTriangleVerticesDataRight)?.position(0)

            Matrix.setIdentityM(mSTMatrix, 0);
            updateVideoMatrices()

            videoWidth = mMediaPlayer.videoWidth.toFloat()
            videoHeight = mMediaPlayer.videoHeight.toFloat()
            mMediaPlayer.setOnVideoSizeChangedListener { mp, width, height ->
                videoWidth = width.toFloat()
                videoHeight = height.toFloat()
                updateVideoMatrices()
            }
        }

        fun setVideoAttributes(videoWidthMm: Float, videoDistanceMm: Float) {
            this.videoWidthMm = videoWidthMm
            this.videoDistanceMm = videoDistanceMm
            updateVideoMatrices()
        }

        fun updateVideoMatrices() {
            val videoWidthMm = if (this.videoWidthMm > 0) this.videoWidthMm else 1.0f
            val videoWidth = if (this.videoWidth > 0) this.videoWidth / 2 else 1.0f
            val videoHeight = if (this.videoHeight > 0) this.videoHeight else 1.0f
            val halfViewWidth = viewWidth / 2
            val halfViewWidthMm = halfViewWidth.pxToMm
            val scaleH = videoHeight / videoWidth / viewHeight * viewWidth / 2
            val scaleMm = videoWidthMm / halfViewWidthMm
            val translatePx = (halfViewWidth - videoDistanceMm.mmToPx)
            val translateX = translatePx / halfViewWidth

            Log.e("Green", "Green updateVideoMatrices $videoWidthMm | $halfViewWidthMm | $videoWidth | $videoHeight | $viewWidth | $viewHeight | $scaleH | $translateX")

            // Update left viewport matrix
            Matrix.setIdentityM(mMVPMatrix, 0)
            Matrix.scaleM(mMVPMatrix, 0, 1f, scaleH, 1f)
            Matrix.translateM(mMVPMatrix, 0, translateX, 0f, 0f)
            Matrix.scaleM(mMVPMatrix, 0, scaleMm, scaleMm, 1f)

            // Update right viewport matrix
            Matrix.setIdentityM(mMVPMatrixRight, 0)
            Matrix.scaleM(mMVPMatrixRight, 0, 1f, scaleH, 1f)
            Matrix.translateM(mMVPMatrixRight, 0, -translateX, 0f, 0f)
            Matrix.scaleM(mMVPMatrixRight, 0, scaleMm, scaleMm, 1f)
        }

        override fun onSurfaceCreated(glUnused: GL10?, config: EGLConfig?) {
            mProgram = createProgram(mVertexShader, mFragmentShader)
            if (mProgram == 0) {
                return
            }
            maPositionHandle = GLES20.glGetAttribLocation(mProgram, "aPosition")
            checkGlError("glGetAttribLocation aPosition")
            if (maPositionHandle == -1) {
                throw RuntimeException("Could not get attrib location for aPosition")
            }
            maTextureHandle = GLES20.glGetAttribLocation(mProgram, "aTextureCoord")
            checkGlError("glGetAttribLocation aTextureCoord")
            if (maTextureHandle == -1) {
                throw RuntimeException("Could not get attrib location for aTextureCoord")
            }
            muMVPMatrixHandle = GLES20.glGetUniformLocation(mProgram, "uMVPMatrix")
            checkGlError("glGetUniformLocation uMVPMatrix")
            if (muMVPMatrixHandle == -1) {
                throw RuntimeException("Could not get attrib location for uMVPMatrix")
            }
            muSTMatrixHandle = GLES20.glGetUniformLocation(mProgram, "uSTMatrix")
            checkGlError("glGetUniformLocation uSTMatrix")
            if (muSTMatrixHandle == -1) {
                throw RuntimeException("Could not get attrib location for uSTMatrix")
            }
            val textures = IntArray(1)
            GLES20.glGenTextures(1, textures, 0)
            mTextureID = textures[0]
            GLES20.glBindTexture(GL_TEXTURE_EXTERNAL_OES, mTextureID)
            checkGlError("glBindTexture mTextureID")
            GLES20.glTexParameterf(
                GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MIN_FILTER,
                GLES20.GL_NEAREST.toFloat()
            )
            GLES20.glTexParameterf(
                GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MAG_FILTER,
                GLES20.GL_LINEAR.toFloat()
            )

            /*
             * Create the SurfaceTexture that will feed this textureID,
             * and pass it to the MediaPlayer
             */mSurface = SurfaceTexture(mTextureID)
            mSurface!!.setOnFrameAvailableListener(this)
            val surface = Surface(mSurface)
            mMediaPlayer!!.setSurface(surface)
            surface.release()
            try {
                mMediaPlayer!!.prepare()
            } catch (t: IOException) {
                Log.e(TAG, "media player prepare failed")
            }
            synchronized(this) { updateSurface = false }
            mMediaPlayer!!.start()
        }

        override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
            // TODO: Recalculate the vertices
            viewWidth = width.toFloat()
            viewHeight = height.toFloat()
            Log.e("Green", "Green onSurfaceChanged: " + viewWidth)
            updateVideoMatrices()
        }

        override fun onDrawFrame(glUnused: GL10?) {
            var needUpdate = false
            synchronized(this) {
                if (updateSurface) {
                    needUpdate = true
                    updateSurface = false
                }
            }
            if (needUpdate) {
                mSurface!!.updateTexImage()
                mSurface!!.getTransformMatrix(mSTMatrix)
                updateSurface = false
            }
            GLES20.glClearColor(0.0f, 0.0f, 0.0f, 1.0f)
            GLES20.glClear(GLES20.GL_DEPTH_BUFFER_BIT or GLES20.GL_COLOR_BUFFER_BIT)
            GLES20.glUseProgram(mProgram)
            checkGlError("glUseProgram")
            GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
            GLES20.glBindTexture(GL_TEXTURE_EXTERNAL_OES, mTextureID)

            renderFrame(false)
            renderFrame(true)

            GLES20.glFinish()
        }

        private fun renderFrame(isRight: Boolean) {
            if (isRight)
                GLES20.glViewport((viewWidth / 2).toInt(), 0, (viewWidth / 2).toInt(), viewHeight.toInt())
            else
                GLES20.glViewport(0, 0, (viewWidth / 2).toInt(), viewHeight.toInt())
            val buffer = if (isRight) mTriangleVerticesRight else mTriangleVertices
            val mvpMatrix = if (isRight) mMVPMatrixRight else mMVPMatrix

            buffer!!.position(TRIANGLE_VERTICES_DATA_POS_OFFSET)
            GLES20.glVertexAttribPointer(
                maPositionHandle, 3, GLES20.GL_FLOAT, false,
                TRIANGLE_VERTICES_DATA_STRIDE_BYTES, buffer
            )
            checkGlError("glVertexAttribPointer maPosition")
            GLES20.glEnableVertexAttribArray(maPositionHandle)
            checkGlError("glEnableVertexAttribArray maPositionHandle")
            buffer!!.position(TRIANGLE_VERTICES_DATA_UV_OFFSET)
            GLES20.glVertexAttribPointer(
                maTextureHandle, 3, GLES20.GL_FLOAT, false,
                TRIANGLE_VERTICES_DATA_STRIDE_BYTES, buffer
            )
            checkGlError("glVertexAttribPointer maTextureHandle")
            GLES20.glEnableVertexAttribArray(maTextureHandle)
            checkGlError("glEnableVertexAttribArray maTextureHandle")
            GLES20.glUniformMatrix4fv(muMVPMatrixHandle, 1, false, mvpMatrix, 0)
            GLES20.glUniformMatrix4fv(muSTMatrixHandle, 1, false, mSTMatrix, 0)
            GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4)
            checkGlError("glDrawArrays")
        }

        private fun loadShader(shaderType: Int, source: String): Int {
            var shader = GLES20.glCreateShader(shaderType)
            if (shader != 0) {
                GLES20.glShaderSource(shader, source)
                GLES20.glCompileShader(shader)
                val compiled = IntArray(1)
                GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, compiled, 0)
                if (compiled[0] == 0) {
                    Log.e(TAG, "Could not compile shader $shaderType:")
                    Log.e(TAG, GLES20.glGetShaderInfoLog(shader))
                    GLES20.glDeleteShader(shader)
                    shader = 0
                }
            }
            return shader
        }

        override fun onFrameAvailable(surfaceTexture: SurfaceTexture?) {
            synchronized(this) {
                updateSurface = true
            }
        }

        private fun createProgram(vertexSource: String, fragmentSource: String): Int {
            val vertexShader: Int = loadShader(GLES20.GL_VERTEX_SHADER, vertexSource)
            if (vertexShader == 0) {
                return 0
            }
            val pixelShader: Int = loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentSource)
            if (pixelShader == 0) {
                return 0
            }
            var program = GLES20.glCreateProgram()
            if (program != 0) {
                GLES20.glAttachShader(program, vertexShader)
                checkGlError("glAttachShader")
                GLES20.glAttachShader(program, pixelShader)
                checkGlError("glAttachShader")
                GLES20.glLinkProgram(program)
                val linkStatus = IntArray(1)
                GLES20.glGetProgramiv(program, GLES20.GL_LINK_STATUS, linkStatus, 0)
                if (linkStatus[0] != GLES20.GL_TRUE) {
                    Log.e(TAG, "Could not link program: ")
                    Log.e(TAG, GLES20.glGetProgramInfoLog(program))
                    GLES20.glDeleteProgram(program)
                    program = 0
                }
            }
            return program
        }

        private fun checkGlError(op: String) {
            var error: Int
            while (GLES20.glGetError().also { error = it } != GLES20.GL_NO_ERROR) {
                Log.e(TAG, "$op: glError $error")
                throw RuntimeException("$op: glError $error")
            }
        }
    }
}