package org.rfcx.companion.view.detail

interface MapPickerProtocol {
    fun startLocationPage(latitude: Double, longitude: Double, altitude: Double, name: String, fromPicker: Boolean)
    fun onSelectedLocation(latitude: Double, longitude: Double, siteId: Int, name: String)
}
