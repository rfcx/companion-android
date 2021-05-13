package org.rfcx.companion.view.detail

interface MapPickerProtocol {
    fun onSelectedLocation(latitude: Double, longitude: Double, siteId: Int, name: String)
}
