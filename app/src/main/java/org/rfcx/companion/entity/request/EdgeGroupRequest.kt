package org.rfcx.companion.entity.request

import org.rfcx.companion.entity.LocationGroups
import java.util.*

data class EdgeGroupRequest(
    var name: String? = null,
    var color: String? = null,
    val deletedAt: Date? = null
)

fun LocationGroups.toRequestBody(): EdgeGroupRequest {
    return EdgeGroupRequest(
        name = this.name,
        color = this.color,
        deletedAt = this.deletedAt
    )
}
