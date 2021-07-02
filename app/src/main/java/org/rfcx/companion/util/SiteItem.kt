package org.rfcx.companion.util

import android.content.Context
import android.location.Location
import android.location.LocationManager
import org.rfcx.companion.R
import org.rfcx.companion.entity.Deployment
import org.rfcx.companion.entity.Locate
import org.rfcx.companion.entity.guardian.GuardianDeployment
import org.rfcx.companion.view.deployment.locate.SiteWithLastDeploymentItem
import java.util.*
import kotlin.collections.ArrayList

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
    deployments: List<Deployment>,
    guardianDeployments: List<GuardianDeployment>,
    projectName: String,
    currentUserLocation: Location,
    locations: List<Locate>
): ArrayList<SiteWithLastDeploymentItem> {
    var showDeployments = deployments
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

    val locationsItems: List<SiteWithLastDeploymentItem> =
        nearLocations?.map {
            SiteWithLastDeploymentItem(
                it.first,
                isHaveDeployment(guardianShowDeployments, showDeployments, it.first),
                it.second
            )
        } ?: listOf()

    val sortDate = locationsItems.filter { it.date != null }.sortedByDescending { it.date }
    val notDeployment = locationsItems.filter { it.date == null }

    val allItems = ArrayList(sortDate + notDeployment)
    return allItems
}

fun isHaveDeployment(
    guardians: List<GuardianDeployment>,
    audioMoths: List<Deployment>,
    locate: Locate
): Date? {
    val guardianDeployAt = guardians.find { dp -> dp.stream?.name == locate.name }?.deployedAt
    val audioMothDeployAt = audioMoths.find { dp -> dp.stream?.name == locate.name }?.deployedAt

    return if (audioMothDeployAt != null && guardianDeployAt != null) {
        if (audioMothDeployAt.time > guardianDeployAt.time) audioMothDeployAt else guardianDeployAt
    } else audioMothDeployAt ?: guardianDeployAt
}

fun getListSiteWithOutCurrentLocation(
    context: Context,
    deployments: List<Deployment>,
    guardianDeployments: List<GuardianDeployment>,
    projectName: String,
    locations: List<Locate>
): ArrayList<SiteWithLastDeploymentItem> {
    var showDeployments = deployments
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

    val locationsItems: List<SiteWithLastDeploymentItem> =
        filterLocations.map {
            SiteWithLastDeploymentItem(
                it,
                isHaveDeployment(guardianShowDeployments, showDeployments, it),
                null
            )
        }

    val sortDate = locationsItems.filter { it.date != null }.sortedByDescending { it.date }
    val notDeployment = locationsItems.filter { it.date == null }
    return ArrayList(sortDate + notDeployment)
}

