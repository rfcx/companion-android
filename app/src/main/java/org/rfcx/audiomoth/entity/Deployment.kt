package org.rfcx.audiomoth.entity

import java.util.*

data class Deployment(
    val batteryDepletedAt: Date = Date(),
    val deployedAt: Date = Date(),
    val batteryLevel: Int = 0,
    val isLatest: Boolean = false,
    val configuration: Configuration = Configuration.default(),
    val location: LocationInDeployment = LocationInDeployment.default(),
    val profileId: String = ""
) {
    companion object {
        const val LAST_DEPLOYMENT = "lastDeployment"
    }
}