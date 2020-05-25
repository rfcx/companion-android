package org.rfcx.audiomoth.entity

data class Locate(
    val lastDeployment: String = "",
    val name: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0
)

data class LocateItem(var locate: Locate?, var docId: String)