package org.rfcx.audiomoth.entity

import java.io.Serializable
import java.sql.Timestamp


open class Device(
    val deviceId: String,
    val siteId: String,
    val siteName: String,
    val deployedAt: Timestamp,
    val location: LatLong,
    val locationName: String,
    val batteryLevel: Int,
    val batteryPredictedUntil: Timestamp,
    val configuration: Stream
) : Serializable {
    companion object {
        const val FIELD_DEVICE_ID = "deviceId"
    }
}

open class Stream(
    val streamName: String,
    val gain: Int,
    val sampleRate: Int,
    val customRecordingPeriod: Boolean,
    val recordingDuration: Int,
    val sleepDuration: Int,
    val recordingPeriodList: ArrayList<String>,
    val durationSelected: String
) : Serializable

open class LatLong(
    val lat: Double,
    val lng: Double
) : Serializable