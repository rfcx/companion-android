package org.rfcx.companion.entity.response

import java.util.*

data class GuardianSoftwareResponse(
    val role: String,
    val version: String,
    val sha1: String,
    val size: Long,
    val url: String,
    val released: Date
)
