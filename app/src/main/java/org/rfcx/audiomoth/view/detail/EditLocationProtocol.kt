package org.rfcx.audiomoth.view.detail

interface EditLocationProtocol {
    fun startMapPickerPage(latitude: Double, longitude: Double, name: String)
    fun startDetailDeploymentPage()
}