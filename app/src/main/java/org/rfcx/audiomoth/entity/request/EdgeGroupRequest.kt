package org.rfcx.audiomoth.entity.request

import org.rfcx.audiomoth.entity.LocationGroups

data class EdgeGroupRequest(
    var name: String = "",
    var color: String = ""

)

fun LocationGroups.toRequestBody(): EdgeGroupRequest {
    return EdgeGroupRequest(
        name = this.name,
        color = this.color
    )
}
