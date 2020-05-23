package org.rfcx.audiomoth.entity

data class LocationInDeployment(
    val id: String = "",
    val name: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0
) {
    companion object {
        fun default() = LocationInDeployment(
            id = "",
            name = "",
            latitude = 0.0,
            longitude = 0.0
        )
    }
}