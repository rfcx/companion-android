package org.rfcx.companion.view.map

data class SiteMarker(
    val id: Int,
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val pin: String
)
