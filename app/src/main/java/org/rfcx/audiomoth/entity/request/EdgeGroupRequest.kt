package org.rfcx.audiomoth.entity.request

import org.rfcx.audiomoth.entity.LocationGroups
import java.util.*

data class EdgeGroupRequest(
    var name: String = "",
    var color: String = "",
    val deletedAt: Date? = null
)

fun LocationGroups.toRequestBody(): EdgeGroupRequest {
    return EdgeGroupRequest(
        name = this.name,
        color = this.color,
        deletedAt = this.deletedAt
    )
}
