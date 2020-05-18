package org.rfcx.audiomoth.entity

import java.io.Serializable

open class LocationInDeployment(
    val id: String = "",
    val name: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0
) : Serializable