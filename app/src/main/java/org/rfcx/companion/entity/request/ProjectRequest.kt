package org.rfcx.companion.entity.request

import org.rfcx.companion.entity.LocationGroup

data class ProjectRequest(
    var name: String? = null,
    var color: String? = null,
    var id: String? = null
)

fun LocationGroup.toRequestBody(): ProjectRequest {
    return ProjectRequest(
        name = this.name,
        color = this.color,
        id = this.coreId
    )
}
