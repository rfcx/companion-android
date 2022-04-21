package org.rfcx.companion.view.detail

import org.rfcx.companion.entity.Project

interface EditLocationActivityListener {
    fun startMapPickerPage(latitude: Double, longitude: Double, altitude: Double, name: String)
    fun updateDeploymentDetail(name: String, altitude: Double)

    fun getLocationGroupName(): String
    fun getLocationGroup(name: String): Project

    fun startLocationGroupPage()

    fun showAppbar()
    fun hideAppbar()
}
