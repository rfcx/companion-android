package org.rfcx.companion.entity.response

import com.google.gson.annotations.SerializedName

class ProjectByIdResponse (
    var id: String? = null,

    var name: String? = null,

    @SerializedName("min_latitude")
    var minLatitude: Double? = null,

    @SerializedName("min_longitude")
    var minLongitude: Double? = null,

    @SerializedName("max_latitude")
    var maxLatitude: Double? = null,

    @SerializedName("max_longitude")
    var maxLongitude: Double? = null
)
