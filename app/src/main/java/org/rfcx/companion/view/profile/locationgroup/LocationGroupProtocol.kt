package org.rfcx.companion.view.profile.locationgroup

import org.rfcx.companion.entity.Project

interface LocationGroupProtocol {
    fun onCreateNewGroup()
    fun onLocationGroupClick(group: Project)
}
