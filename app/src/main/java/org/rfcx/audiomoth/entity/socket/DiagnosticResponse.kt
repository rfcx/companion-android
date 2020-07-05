package org.rfcx.audiomoth.entity.socket

import com.google.gson.JsonArray
import com.google.gson.annotations.SerializedName
import org.rfcx.audiomoth.entity.guardian.GuardianConfiguration

data class DiagnosticResponse(
    val diagnostic: Diagnostic,
    val configure: GuardianConfiguration,
    val prefs: JsonArray
) : SocketResposne

data class Diagnostic(
    @SerializedName("total_local")
    val totalLocal: Int,
    @SerializedName("total_checkin")
    val totalCheckIn: Int,
    @SerializedName("total_record_time")
    val totalRecordTime: Int,
    @SerializedName("total_file_size")
    val totalFileSize: Int,
    @SerializedName("battery_percentage")
    val batteryPercentage: Int
)

fun Diagnostic.getRecordTime(): String {
    var amountTime = ""
    val minutes = this.totalRecordTime / 60
    amountTime = if (minutes > 60L) {
        val hours = minutes / 60
        val min = minutes % 60
        if (min == 0) {
            "$hours hours"
        } else {
            "$hours hours $min minutes"
        }
    } else {
        "$minutes minutes"
    }

    return amountTime
}
