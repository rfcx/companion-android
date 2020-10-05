package org.rfcx.audiomoth.entity.response

import org.rfcx.audiomoth.entity.LocationGroups
import org.rfcx.audiomoth.entity.SyncState

data class LocationGroupResponse(
    var name: String = "",
    var color: String = ""
)

fun LocationGroupResponse.toLocationGroup(): LocationGroups {
    return LocationGroups(
        name = this.name,
        color = this.color,
        syncState = SyncState.Sent.key
    )
}
