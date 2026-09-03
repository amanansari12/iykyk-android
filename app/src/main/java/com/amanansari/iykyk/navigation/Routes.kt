package com.amanansari.iykyk.navigation

import android.net.Uri
import kotlinx.serialization.Serializable

@Serializable
object Home


@Serializable
data class Processing(
    val uri: String
)

@Serializable
object Results