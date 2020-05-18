package org.rfcx.audiomoth.entity

import java.io.Serializable

open class Configuration(
    val gain: Int = 0,
    val sampleRate: Int = 0
) : Serializable