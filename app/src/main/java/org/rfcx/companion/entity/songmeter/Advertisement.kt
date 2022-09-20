package org.rfcx.companion.entity.songmeter

import java.io.Serializable

data class Advertisement(
    var prefixes: String,
    val serialName: String,
    val address: String,
    var isReadyToPair: Boolean
): Serializable
