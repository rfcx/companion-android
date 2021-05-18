package org.rfcx.companion.entity.response

import org.rfcx.companion.entity.LocationGroup
import org.rfcx.companion.entity.Project

/**
 * Firestore response for getting a locationGroups
 */
data class ProjectResponse(
    var id: String? = null,
    var name: String = "",
    var color: String = ""
)

fun ProjectResponse.toLocationGroups(): Project {
    return Project(
        name = this.name,
        color = this.color,
        serverId = this.id
    )
}

fun ProjectResponse.toLocationGroup(): LocationGroup {
    return LocationGroup(
        name = this.name,
        color = this.color,
        coreId = this.id
    )
}

fun LocationGroup.toLocationGroupsResponse(): ProjectResponse {
    return ProjectResponse(
        name = this.name ?: "",
        color = this.color ?: "",
        id = this.coreId
    )
}
