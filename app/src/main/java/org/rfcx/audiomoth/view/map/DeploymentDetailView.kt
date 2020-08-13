package org.rfcx.audiomoth.view.map

import org.rfcx.audiomoth.entity.EdgeDeployment
import org.rfcx.audiomoth.entity.guardian.GuardianDeployment
import java.util.*

sealed class DeploymentDetailView {
    abstract val id: Int // edge deployment local id
    abstract val locationName: String
    abstract val latitude: Double
    abstract val longitude: Double
    abstract val viewType: Int


    data class EdgeDeploymentView(
        override val id: Int, // edge deployment local id
        var serverId: String?,
        val deploymentId: String,
        val batteryDepletedAt: Date,
        val deployedAt: Date = Date(),
        val batteryLevel: Int,
        val state: Int,
        val createdAt: Date,
        val syncState: Int,
        val updatedAt: Date?,
        val deletedAt: Date?,
        override val locationName: String,
        override val latitude: Double,
        override val longitude: Double,
        override val viewType: Int = DeploymentViewPagerAdapter.DEPLOYMENT_EDGE_VIEW
    ) : DeploymentDetailView()

    data class GuardianDeploymentView(
        override val id: Int,
        var serverId: String? = null,
        val deployedAt: Date,
        val state: Int,
        val wifiName: String?,
        val createdAt: Date,
        val syncState: Int,
        override val locationName: String,
        override val latitude: Double,
        override val longitude: Double,
        override val viewType: Int = DeploymentViewPagerAdapter.DEPLOYMENT_GUARDIAN_VIEW
    ) : DeploymentDetailView()
}

fun EdgeDeployment.toEdgeDeploymentView(): DeploymentDetailView.EdgeDeploymentView {
    return DeploymentDetailView.EdgeDeploymentView(
        id = this.id,
        serverId = this.serverId,
        deploymentId = this.deploymentId ?: "",
        deployedAt = this.deployedAt,
        batteryDepletedAt = this.batteryDepletedAt,
        batteryLevel = this.batteryLevel,
        state = this.state,
        createdAt = this.createdAt,
        syncState = this.syncState,
        updatedAt = this.updatedAt,
        deletedAt = this.deletedAt,
        locationName = this.location?.name ?: "",
        latitude = this.location?.latitude ?: 0.0,
        longitude = this.location?.longitude ?: 0.0
    )
}

fun GuardianDeployment.toGuardianDeploymentView(): DeploymentDetailView.GuardianDeploymentView {
    return DeploymentDetailView.GuardianDeploymentView(
        id = this.id,
        serverId = this.serverId,
        deployedAt = this.deployedAt,
        state = this.state,
        wifiName = this.wifiName,
        createdAt = this.createdAt,
        syncState = this.syncState,
        locationName = this.location?.name ?: "",
        latitude = this.location?.latitude ?: 0.0,
        longitude = this.location?.longitude ?: 0.0
    )
}
