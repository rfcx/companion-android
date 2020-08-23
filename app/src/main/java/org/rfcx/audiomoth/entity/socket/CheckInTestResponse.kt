package org.rfcx.audiomoth.entity.socket

import com.google.gson.annotations.SerializedName

data class CheckInTestResponse(
    val checkin: CheckIn = CheckIn()
) : SocketResposne

data class CheckIn(
    @SerializedName("api_url")
    val apiUrl: String = "-",
    val state: String = "-",
    @SerializedName("delivery_time")
    val deliveryTime: String = "-"
)
