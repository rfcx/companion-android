package org.rfcx.companion.view.detail

import org.rfcx.companion.entity.LocationGroup

interface EditLocationActivityListener {
    fun startMapPickerPage(latitude: Double, longitude: Double, name: String)
    fun updateDeploymentDetail(name: String, altitude: Double)

    fun getLocationGroupName(): String
    fun getLocationGroup(name: String): LocationGroup

    fun startLocationGroupPage()

    fun showAppbar()
    fun hideAppbar()
}
