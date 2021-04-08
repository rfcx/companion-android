package org.rfcx.companion.util

import android.content.Context
import android.location.Location
import android.location.LocationManager
import org.rfcx.companion.R
import org.rfcx.companion.entity.EdgeDeployment
import org.rfcx.companion.entity.Locate
import org.rfcx.companion.entity.guardian.GuardianDeployment
import org.rfcx.companion.view.deployment.locate.SiteWithLastDeploymentItem

private fun findNearLocations(
    locateItems: ArrayList<Locate>,
    currentUserLocation: Location
): List<Pair<Locate, Float>>? {
    if (locateItems.isNotEmpty()) {
        val itemsWithDistance = arrayListOf<Pair<Locate, Float>>()
        // Find locate distances
        locateItems.mapTo(itemsWithDistance, {
            val loc = Location(LocationManager.GPS_PROVIDER)
            loc.latitude = it.latitude
            loc.longitude = it.longitude
            val distance = loc.distanceTo(currentUserLocation) // return in meters
            Pair(it, distance)
        })
        return itemsWithDistance
    }
    return null
}

fun getListSite(
    context: Context,
    edgeDeployments: List<EdgeDeployment>,
    guardianDeployments: List<GuardianDeployment>,
    projectName: String,
    currentUserLocation: Location,
    locations: List<Locate>
): ArrayList<SiteWithLastDeploymentItem> {
    var showDeployments = edgeDeployments
    var guardianShowDeployments = guardianDeployments
    if (projectName != context.getString(R.string.none)) {
        showDeployments =
            showDeployments.filter { it.stream?.project?.name == projectName }
        guardianShowDeployments =
            guardianDeployments.filter { it.stream?.project?.name == projectName }
    }
    val nearLocations =
        findNearLocations(ArrayList(locations.filter { loc ->
            loc.locationGroup?.name == projectName || projectName == context.getString(
                R.string.none
            )
        }), currentUserLocation)?.sortedBy { it.second }

    val edgeLocationsItems: List<SiteWithLastDeploymentItem> =
        nearLocations?.map {
            SiteWithLastDeploymentItem(
                it.first,
                showDeployments.find { dp -> dp.stream?.name == it.first.name }?.deployedAt,
                it.second
            )
        } ?: listOf()

    val guardianLocationItems: List<SiteWithLastDeploymentItem> =
        nearLocations?.map {
            SiteWithLastDeploymentItem(
                it.first,
                guardianShowDeployments.find { dp -> dp.stream?.name == it.first.name }?.deployedAt,
                it.second
            )
        } ?: listOf()

    val sortDate = (edgeLocationsItems + guardianLocationItems).filter { it.date != null }
        .sortedByDescending { it.date }
    val notDeployment = (edgeLocationsItems + guardianLocationItems).filter { it.date == null }

    return ArrayList(sortDate + notDeployment)
}

fun getListSiteWithOutCurrentLocation(
    context: Context,
    edgeDeployments: List<EdgeDeployment>,
    guardianDeployments: List<GuardianDeployment>,
    projectName: String,
    locations: List<Locate>
): ArrayList<SiteWithLastDeploymentItem> {
    var showDeployments = edgeDeployments
    var guardianShowDeployments = guardianDeployments
    if (projectName != context.getString(R.string.none)) {
        showDeployments =
            showDeployments.filter { it.stream?.project?.name == projectName }
        guardianShowDeployments =
            guardianDeployments.filter { it.stream?.project?.name == projectName }
    }

    val filterLocations = ArrayList(locations.filter { loc ->
        loc.locationGroup?.name == projectName || projectName == context.getString(
            R.string.none
        )
    })

    val edgeLocationsItems: List<SiteWithLastDeploymentItem> =
        filterLocations.map {
            SiteWithLastDeploymentItem(
                it,
                showDeployments.find { dp -> dp.stream?.name == it.name }?.deployedAt,
                null
            )
        }

    val guardianLocationItems: List<SiteWithLastDeploymentItem> =
        filterLocations.map {
            SiteWithLastDeploymentItem(
                it,
                guardianShowDeployments.find { dp -> dp.stream?.name == it.name }?.deployedAt,
                null
            )
        }

    val sortDate = (edgeLocationsItems + guardianLocationItems).filter { it.date != null }
        .sortedByDescending { it.date }
    val notDeployment = (edgeLocationsItems + guardianLocationItems).filter { it.date == null }
    return ArrayList(sortDate + notDeployment)
}

