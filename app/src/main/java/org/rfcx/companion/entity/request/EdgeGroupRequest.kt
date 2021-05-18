package org.rfcx.companion.entity.request

import org.rfcx.companion.entity.Project
import java.util.*

data class EdgeGroupRequest(
    var name: String? = null,
    var color: String? = null,
    val deletedAt: Date? = null
)

fun Project.toRequestBody(): EdgeGroupRequest {
    return EdgeGroupRequest(
        name = this.name,
        color = this.color,
        deletedAt = this.deletedAt
    )
}
