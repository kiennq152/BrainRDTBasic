package com.example.brainrdtbasic.opengl

interface IStereoVideoView {
    fun setVideoAttributes(videoWidthMm: Float, videoDistanceMm: Float)
    fun setIotdValue(iotdValue: Int)
}