package org.rfcx.companion.util

import android.content.Context
import android.location.Location
import android.location.LocationManager
import org.rfcx.companion.R
import org.rfcx.companion.entity.Stream
import org.rfcx.companion.entity.guardian.Deployment
import org.rfcx.companion.view.deployment.locate.SiteWithLastDeploymentItem
import java.util.*
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
    context: Context,
    deploymentList: List<Deployment>,
    projectName: String,
    currentUserLocation: Location,
    locations: List<Stream>
): ArrayList<SiteWithLastDeploymentItem> {
    var deployments = deploymentList
    if (projectName != context.getString(R.string.none)) {
        deployments =
            deploymentList.filter { it.stream?.project?.name == projectName }
    }
    val nearLocations =
        findNearLocations(
            ArrayList(
                locations.filter { loc ->
                    loc.project?.name == projectName || projectName == context.getString(
                        R.string.none
                    )
                }
            ),
            currentUserLocation
        )?.sortedBy { it.second }

    val locationsItems: List<SiteWithLastDeploymentItem> =
        nearLocations?.map {
            SiteWithLastDeploymentItem(
                it.first,
                isHaveDeployment(deployments, it.first),
                it.second
            )
        } ?: listOf()

    return ArrayList(locationsItems)
}

fun isHaveDeployment(
    deployments: List<Deployment>,
    locate: Stream
): Date? {
    return deployments.find { dp -> dp.stream?.name == locate.name }?.deployedAt
}

fun getListSiteWithOutCurrentLocation(
    context: Context,
    deployments: List<Deployment>,
    projectName: String,
    locations: List<Stream>
): ArrayList<SiteWithLastDeploymentItem> {
    var showDeployments = deployments
    if (projectName != context.getString(R.string.none)) {
        showDeployments =
            deployments.filter { it.stream?.project?.name == projectName }
    }

    val filterLocations = ArrayList(
        locations.filter { loc ->
            loc.project?.name == projectName || projectName == context.getString(
                R.string.none
            )
        }
    )

    val locationsItems: List<SiteWithLastDeploymentItem> =
        filterLocations.map {
            SiteWithLastDeploymentItem(
                it,
                null,
                null
            )
        }

    val sortDate = locationsItems.filter { it.date != null }.sortedByDescending { it.date }
    val notDeployment = locationsItems.filter { it.date == null }
    return ArrayList(sortDate + notDeployment)
}
