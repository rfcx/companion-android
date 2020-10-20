package org.rfcx.companion.entity.response

import org.rfcx.companion.entity.LocationGroups
import java.util.*

/**
 * Firestore response for getting a locationGroups
 */
data class LocationGroupsResponse(
    var serverId: String? = null,
    var name: String = "",
    var color: String = "",
    var deletedAt: Date? = null
)

fun LocationGroupsResponse.toLocationGroups(): LocationGroups {
    return LocationGroups(
        name = this.name,
        color = this.color,
        serverId = this.serverId,
        deletedAt = this.deletedAt
    )
}
