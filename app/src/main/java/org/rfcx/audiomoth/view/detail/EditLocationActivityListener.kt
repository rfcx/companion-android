package org.rfcx.audiomoth.view.detail

import org.rfcx.audiomoth.entity.LocationGroup

interface EditLocationActivityListener {
    fun startMapPickerPage(latitude: Double, longitude: Double, name: String)
    fun updateDeploymentDetail(name: String)
    fun getLocationGroupName(): String
    fun getLocationGroup(name: String): LocationGroup
    fun showAppbar()
    fun hideAppbar()
}
