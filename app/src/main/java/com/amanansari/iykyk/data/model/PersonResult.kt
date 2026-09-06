package com.amanansari.iykyk.data.model

import android.graphics.Bitmap

data class PersonResult(
    val clusterId: Int,
    val appearanceCount: Int,
    val representativeFace: Bitmap
)