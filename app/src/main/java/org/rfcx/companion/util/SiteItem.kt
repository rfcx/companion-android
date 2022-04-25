package org.rfcx.companion.util

import android.content.Context
import android.location.Location
import android.location.LocationManager
import org.rfcx.companion.R
import org.rfcx.companion.entity.Stream
import org.rfcx.companion.view.deployment.locate.SiteWithLastDeploymentItem
import kotlin.collections.ArrayList

private fun findNearLocations(
    streamItems: List<Stream>,
    currentUserLocation: Location
): List<Pair<Stream, Float>>? {
    if (streamItems.isNotEmpty()) {
        // Find locate distances
        return streamItems.map {
            val loc = Location(LocationManager.GPS_PROVIDER)
            loc.latitude = it.latitude
            loc.longitude = it.longitude
            val distance = loc.distanceTo(currentUserLocation) // return in meters
            Pair(it, distance)
        }
    }
    return null
}

fun getListSite(
    currentUserLocation: Location,
    streams: List<Stream>
): List<SiteWithLastDeploymentItem> {

    val nearLocations =
        findNearLocations(
            streams,
            currentUserLocation
        )?.sortedBy { it.second }

    val locationsItems: List<SiteWithLastDeploymentItem> =
        nearLocations?.map { it ->
            val deployment = it.first.getActiveDeployment()
            SiteWithLastDeploymentItem(
                it.first,
                deployment?.deployedAt,
                it.second
            )
        } ?: listOf()

    return locationsItems
}

fun getListSiteWithOutCurrentLocation(
    streams: List<Stream>
): List<SiteWithLastDeploymentItem> {
    val locationsItems: List<SiteWithLastDeploymentItem> =
        streams.map {
            val deployment = it.getActiveDeployment()
            SiteWithLastDeploymentItem(
                it,
                deployment?.deployedAt,
                null
            )
        }

    return locationsItems.sortedByDescending { it.date }
}
