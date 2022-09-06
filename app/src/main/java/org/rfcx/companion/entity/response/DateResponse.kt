package org.rfcx.companion.entity.response

import com.google.gson.annotations.SerializedName

data class DateResponse(
    @SerializedName("_seconds")
    val seconds: Long = 0,

    @SerializedName("_nanoseconds")
    val nanoseconds: Int = 0
)
