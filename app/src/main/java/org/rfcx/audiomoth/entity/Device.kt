package org.rfcx.audiomoth.entity

import java.io.Serializable
import java.sql.Timestamp


open class Device(
    val deviceId: String,
    val deployedAt: Timestamp,
    val location: LatLong,
    val locationName: String,
    val batteryLevel: Int,
    val batteryPredictedUntil: Timestamp,
    val configuration: Stream
) : Serializable

open class Stream(
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