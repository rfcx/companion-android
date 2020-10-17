package org.rfcx.audiomoth.view.profile.locationgroup

import org.rfcx.audiomoth.entity.LocationGroups

interface LocationGroupProtocol {
    fun onCreateNewGroup()
    fun onLocationGroupClick(group: LocationGroups)
}
