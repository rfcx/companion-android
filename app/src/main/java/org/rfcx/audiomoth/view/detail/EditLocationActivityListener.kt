package org.rfcx.audiomoth.view.detail

interface EditLocationActivityListener {
    fun startMapPickerPage(latitude: Double, longitude: Double, name: String)
    fun startDetailDeploymentPage(name: String)
    fun showAppbar()
    fun hideAppbar()
}