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
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

class StereoIotdVideoView(context: Context, val mediaPlayer: MediaPlayer): GLSurfaceView(context),
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
        videoRenderer.iotd = iotdValue
    }

    class VideoRenderer(val context: Context, val mMediaPlayer: MediaPlayer) : GLSurfaceView.Renderer, SurfaceTexture.OnFrameAvailableListener {
        private val TAG = "VideoRender"
        var iotd:Int = 0
        private val FLOAT_SIZE_BYTES = 4
        private val TRIANGLE_VERTICES_DATA_STRIDE_BYTES = 5 * FLOAT_SIZE_BYTES
        private val TRIANGLE_VERTICES_DATA_POS_OFFSET = 0
        private val TRIANGLE_VERTICES_DATA_UV_OFFSET = 3
        private val mTriangleVerticesData = floatArrayOf(
            // X, Y, Z, U, V
            -1.0f, -1.0f, 0f, 0f, 0f,
            1.0f, -1.0f, 0f, 1f, 0f,
            -1.0f, 1.0f, 0f, 0f, 1f,
            1.0f, 1.0f, 0f, 1f, 1f,
        )
        private val mTriangleVerticesDataRight = floatArrayOf(
            // X, Y, Z, U, V
            -1.0f, -1.0f, 0f, 0f, 0f,
            1.0f, -1.0f, 0f, 1f, 0f,
            -1.0f, 1.0f, 0f, 0f, 1f,
            1.0f, 1.0f, 0f, 1f, 1f,
        )

        private var mTriangleVertices: FloatBuffer? = null
        private var mTriangleVerticesRight: FloatBuffer? = null

        private var mMVPMatrix = FloatArray(16)
        private var mMVPMatrixRight = FloatArray(16)
        private var mMVPMatrixBuffer = FloatArray(16)
        private var mSTMatrix = FloatArray(16)

        private var mSurface: SurfaceTexture? = null
        private var updateSurface = false

        private var GL_TEXTURE_EXTERNAL_OES = 0x8D65

        private var mTextureID = 0

        private var viewWidth = 1.0f
        private var viewHeight = 1.0f
        private var videoWidth = 1.0f
        private var videoHeight = 1.0f

        private var videoWidthMm = 1.0f
        private var videoDistanceMm = 1.0f

        private var videoRenderProgram = VideoRenderProgram()
        private var textureRenderProgram = TextureRenderProgram()
        private var textureBufferQueue = TextureBufferQueue()

        init {
            mTriangleVertices = ByteBuffer.allocateDirect(
                mTriangleVerticesData.count() * FLOAT_SIZE_BYTES)
                .order(ByteOrder.nativeOrder()).asFloatBuffer();
            mTriangleVertices?.put(mTriangleVerticesData)?.position(0)

            mTriangleVerticesRight = ByteBuffer.allocateDirect(
                mTriangleVerticesDataRight.count() * FLOAT_SIZE_BYTES)
                .order(ByteOrder.nativeOrder()).asFloatBuffer()
            mTriangleVerticesRight?.put(mTriangleVerticesDataRight)?.position(0)

            Matrix.setIdentityM(mSTMatrix, 0)
            Matrix.setIdentityM(mMVPMatrixBuffer, 0)
            Matrix.scaleM(mMVPMatrixBuffer, 0, 1f, -1f, 1f)
            updateVideoMatrices()

            videoWidth = mMediaPlayer.videoWidth.toFloat()
            videoHeight = mMediaPlayer.videoHeight.toFloat()
            mMediaPlayer.setOnVideoSizeChangedListener { mp, width, height ->
                videoWidth = width.toFloat()
                videoHeight = height.toFloat()
                updateVideoMatrices()
            }
            mMediaPlayer.setOnSeekCompleteListener {

            }
        }

        fun setVideoAttributes(videoWidthMm: Float, videoDistanceMm: Float) {
            this.videoWidthMm = videoWidthMm
            this.videoDistanceMm = videoDistanceMm
            updateVideoMatrices()
        }

        fun updateVideoMatrices() {
            val videoWidthMm = if (this.videoWidthMm > 0) this.videoWidthMm else 1.0f
            val videoWidth = if (this.videoWidth > 0) this.videoWidth else 1.0f
            val videoHeight = if (this.videoHeight > 0) this.videoHeight else 1.0f
            val halfViewWidth = viewWidth / 2
            val halfViewWidthMm = halfViewWidth.pxToMm
            val scaleH = videoHeight / videoWidth / viewHeight * viewWidth / 2
            val scaleMm = videoWidthMm / halfViewWidthMm
            val translatePx = (halfViewWidth - videoDistanceMm.mmToPx)
            val translateX = translatePx / halfViewWidth

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
            videoRenderProgram.create()
            textureBufferQueue.generateTextures()
            textureRenderProgram.create()
            val textures = IntArray(1)
            GLES20.glGenTextures(1, textures, 0)
            mTextureID = textures[0]
            GLES20.glBindTexture(GL_TEXTURE_EXTERNAL_OES, mTextureID)
            checkGlError("glBindTexture mTextureID")
            GLES20.glTexParameteri(
                GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MIN_FILTER,
                GLES20.GL_NEAREST
            )
            GLES20.glTexParameteri(
                GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MAG_FILTER,
                GLES20.GL_LINEAR
            )

            /*
             * Create the SurfaceTexture that will feed this textureID,
             * and pass it to the MediaPlayer
             */mSurface = SurfaceTexture(mTextureID)
            mSurface!!.setOnFrameAvailableListener(this)
            val surface = Surface(mSurface)
            mMediaPlayer!!.setSurface(surface)
            surface.release()
//            try {
//                mMediaPlayer!!.prepare()
//            } catch (t: IOException) {
//                Log.e(TAG, "media player prepare failed")
//            }
            synchronized(this) { updateSurface = false }
            mMediaPlayer!!.start()
        }

        override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
            // TODO: Recalculate the vertices
            viewWidth = width.toFloat()
            viewHeight = height.toFloat()
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
                // Render to frame buffer first
                textureBufferQueue.pushFrame()
                textureBufferQueue.bindFrameBuffer()
                GLES20.glViewport(0, 0, 1280, 720)
                GLES20.glClearColor(0f, 0f, 1f, 1f)
                GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)
                videoRenderProgram.render(mTriangleVertices, mMVPMatrixBuffer, mTextureID)

                GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0)
            }
            GLES20.glClearColor(0.0f, 0.0f, 0.0f, 1.0f)
            GLES20.glClear(GLES20.GL_DEPTH_BUFFER_BIT or GLES20.GL_COLOR_BUFFER_BIT)

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

            //videoRenderProgram.render(buffer, mvpMatrix, mTextureID)
            val textureId = if (isRight) textureBufferQueue.getTexture(if(iotd>0) iotd else 0) else textureBufferQueue.getTexture(if(iotd<0) iotd else 0)
            textureRenderProgram.render(buffer, mvpMatrix, textureId)
        }

        override fun onFrameAvailable(surfaceTexture: SurfaceTexture?) {
            synchronized(this) {
                updateSurface = true;
            }
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