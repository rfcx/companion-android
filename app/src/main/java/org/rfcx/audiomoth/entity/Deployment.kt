package org.rfcx.audiomoth.entity

import java.io.Serializable
import java.util.*

open class Deployment(
    val batteryDepletedAt: Date = Date(),
    val deployedAt: Date = Date(),
    val batteryLevel: Int = 0,
    val isLatest: Boolean = false,
    val configuration: Configuration = Configuration(0, 0),
    val location: LocationInDeployment = LocationInDeployment("","", 0.0, 0.0),
    val profileId: String = ""
) : Serializable {
    companion object {
        const val FIELD_IS_LATEST = "isLatest"
    }
}