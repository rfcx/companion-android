package org.rfcx.audiomoth.entity

import java.io.Serializable

open class Location(
    val lastDeployment: String = "",
    val name: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0
) : Serializable