package org.rfcx.audiomoth.entity

import java.util.*
import kotlin.collections.ArrayList

data class Deployment(
    val batteryDepletedAt: Date = Date(),
    val deployedAt: Date = Date(),
    val batteryLevel: Int = 0,
    val latest: Boolean = false,
    val configuration: Configuration = Configuration.default(),
    val location: LocationInDeployment = LocationInDeployment.default(),
    val profileId: String = "",
    val photos: ArrayList<String> = arrayListOf()
) {
    companion object {
        const val LAST_DEPLOYMENT = "lastDeployment"
        const val PHOTOS = "photos"
        const val IS_LATEST = "latest"
        const val DEPLOYED_AT = "deployedAt"
    }
}