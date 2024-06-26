package org.rfcx.companion.entity.response

import org.rfcx.companion.entity.Permissions
import org.rfcx.companion.entity.Project

/**
 * Firestore response for getting a locationGroups
 */
data class ProjectResponse(
    var id: String? = null,
    var name: String = "",
    var color: String = "",
    var permissions: List<String> = listOf()
)

fun ProjectResponse.toProject(): Project {
    return Project(
        name = this.name,
        color = this.color,
        serverId = this.id,
        permissions = this.permissionsLabel()
    )
}

fun ProjectResponse.permissionsLabel(): String {
    return if (this.permissions.contains("C") && this.permissions.contains("R") && this.permissions.contains("U") && this.permissions.contains("D")) {
        Permissions.ADMIN.value
    } else if (this.permissions.contains("C") && this.permissions.contains("R") && this.permissions.contains("U")) {
        Permissions.MEMBER.value
    } else {
        Permissions.GUEST.value
    }
}
