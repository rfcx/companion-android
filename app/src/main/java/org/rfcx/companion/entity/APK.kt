package org.rfcx.companion.entity

import org.rfcx.companion.util.file.APKUtils

data class APK(
    val role: String,
    val version: String,
    val status: APKUtils.APKStatus
)
