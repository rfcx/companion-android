package org.rfcx.audiomoth.entity.guardian

import com.google.gson.annotations.SerializedName
import io.realm.RealmModel
import io.realm.annotations.RealmClass

@RealmClass
open class Diagnostic(
    @SerializedName("total_local")
    var totalLocal: Int = 0,
    @SerializedName("total_checkin")
    var totalCheckIn: Int = 0,
    @SerializedName("total_record_time")
    var totalRecordTime: Int = 0,
    @SerializedName("total_file_size")
    var totalFileSize: Int = 0,
    @SerializedName("battery_percentage")
    var batteryPercentage: Int = 0
) : RealmModel {

    fun getRecordTime(): String {
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
}
