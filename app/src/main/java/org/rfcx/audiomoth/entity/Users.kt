package org.rfcx.audiomoth.entity

import java.io.Serializable
import java.sql.Timestamp
import java.util.*

open class User(
    val name: String
) : Serializable {
    companion object {
        const val FIELD_NAME = "name"
    }
}

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

open class Configuration(
    val gain: Int = 0,
    val sampleRate: Int = 0
) : Serializable

open class LocationInDeployment(
    val id: String = "",
    val name: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0
) : Serializable

open class Location(
    val lastDeployment: String = "",
    val name: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0
) : Serializable

open class Profile(
    val gain: Int = 0,
    val name: String = "",
    val sampleRate: Int = 0
) : Serializable

