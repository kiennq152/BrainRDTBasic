package com.example.brainrdtbasic.opengl

import android.opengl.GLES20
import android.util.Log
import java.nio.ByteBuffer
import java.nio.ByteOrder

class TextureBufferQueue {
    val TAG = "com.example.brainrdtbasic.opengl.TextureBufferQueue"
    //private var GLES20.GL_TEXTURE_2D = 0x8D65

    val delayDepth = 8
    var textureArr = IntArray(delayDepth)
    var frameBufferArr = IntArray(delayDepth)
    var curPos = 0

    fun generateTextures() {
        GLES20.glGenFramebuffers(delayDepth, frameBufferArr, 0)
        checkGlError("glGenFramebuffers")
        GLES20.glGenTextures(delayDepth, textureArr, 0)
        checkGlError("glGenTextures")
        for (i in 0..delayDepth-1) {
            val fb = frameBufferArr[i]
            val texture = textureArr[i]
            GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, fb)
            checkGlError("glBindFramebuffer")
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, texture)
            checkGlError("glBindTexture")
            // Texture parameters
            var texBuffer = ByteBuffer.allocateDirect(1280 * 720 * 4).order(ByteOrder.nativeOrder()).asIntBuffer()
            GLES20.glTexImage2D(
                GLES20.GL_TEXTURE_2D, 0,
                GLES20.GL_RGBA, 1280, 720, 0,
                GLES20.GL_RGBA,
                GLES20.GL_UNSIGNED_BYTE, texBuffer);
            checkGlError("glTexImage2D")
            GLES20.glTexParameteri(
                GLES20.GL_TEXTURE_2D,
                GLES20.GL_TEXTURE_MAG_FILTER,
                GLES20.GL_LINEAR);
            checkGlError("glTexParameteri")
            GLES20.glTexParameteri(
                GLES20.GL_TEXTURE_2D,
                GLES20.GL_TEXTURE_MIN_FILTER,
                GLES20.GL_LINEAR);
            checkGlError("glTexParameteri")
            GLES20.glTexParameteri(
                GLES20.GL_TEXTURE_2D,
                GLES20.GL_TEXTURE_WRAP_S,
                GLES20.GL_CLAMP_TO_EDGE);
            checkGlError("glTexParameteri")
            GLES20.glTexParameteri(
                GLES20.GL_TEXTURE_2D,
                GLES20.GL_TEXTURE_WRAP_T,
                GLES20.GL_CLAMP_TO_EDGE);
            checkGlError("glTexParameteri")
            GLES20.glFramebufferTexture2D(GLES20.GL_FRAMEBUFFER, GLES20.GL_COLOR_ATTACHMENT0, GLES20.GL_TEXTURE_2D, texture, 0)
            checkGlError("glFramebufferTexture2D")
        }
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0)
    }

    fun setVideoResolution(width: Int, height: Int) {
        for (texture in textureArr) {
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, texture)
            GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0,
                GLES20.GL_RGBA, width, height, 0,
                GLES20.GL_RGBA,
                GLES20.GL_UNSIGNED_BYTE, null);
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D,
                GLES20.GL_TEXTURE_MAG_FILTER,
                GLES20.GL_LINEAR);
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D,
                GLES20.GL_TEXTURE_MIN_FILTER,
                GLES20.GL_LINEAR);
            GLES20.glTexParameteri(
                GLES20.GL_TEXTURE_2D,
                GLES20.GL_TEXTURE_WRAP_S,
                GLES20.GL_CLAMP_TO_EDGE);
            GLES20.glTexParameteri(
                GLES20.GL_TEXTURE_2D,
                GLES20.GL_TEXTURE_WRAP_T,
                GLES20.GL_CLAMP_TO_EDGE);
        }
    }

    fun getTexture(delay: Int): Int {
        var pos = curPos - delay
        while (pos < 0)
            pos += delayDepth
        pos %= delayDepth
        return textureArr[pos]
    }

    fun bindTexture(delay: Int) {
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, getTexture(delay))
    }

    fun pushFrame() {
        curPos = (curPos + 1) % delayDepth
    }

    fun bindFrameBuffer() {
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, frameBufferArr[curPos])
    }


    private fun checkGlError(op: String) {
        var error: Int
        while (GLES20.glGetError().also { error = it } != GLES20.GL_NO_ERROR) {
            Log.e(TAG, "$op: glError $error")
            throw RuntimeException("$op: glError $error")
        }
    }
}