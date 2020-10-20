package org.rfcx.companion.view.profile.locationgroup

import org.rfcx.companion.entity.LocationGroups

interface LocationGroupProtocol {
    fun onCreateNewGroup()
    fun onLocationGroupClick(group: LocationGroups)
}
