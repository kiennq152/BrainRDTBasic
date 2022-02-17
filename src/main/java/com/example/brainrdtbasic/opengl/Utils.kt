package com.example.brainrdtbasic

import android.content.res.Resources
import android.util.TypedValue

val Number.mmToPx get() = TypedValue.applyDimension(
    TypedValue.COMPLEX_UNIT_MM,
    this.toFloat(),
    Resources.getSystem().displayMetrics)

val Number.pxToMm get() = (this.toFloat() / (1000000).mmToPx * 1000000)