package org.rfcx.audiomoth.entity.response

import org.rfcx.audiomoth.entity.LocationGroups

/**
 * Firestore response for getting a locationGroups
 */
data class LocationGroupsResponse(
    var serverId: String? = null,
    var name: String = "",
    var color: String = ""
)

fun LocationGroupsResponse.toLocationGroups(): LocationGroups {
    return LocationGroups(
        name = this.name,
        color = this.color,
        serverId = this.serverId
    )
}
