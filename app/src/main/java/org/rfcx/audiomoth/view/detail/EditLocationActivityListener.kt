package org.rfcx.audiomoth.view.detail

interface EditLocationActivityListener {
    fun startMapPickerPage(latitude: Double, longitude: Double, name: String)
    fun updateDeploymentDetail(name: String)
    fun showAppbar()
    fun hideAppbar()
}