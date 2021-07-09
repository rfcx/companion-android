package org.rfcx.companion.entity.songmeter

import java.io.Serializable

data class Advertisement(
    val prefixes: String,
    val serialName: String,
    val address: String
): Serializable
