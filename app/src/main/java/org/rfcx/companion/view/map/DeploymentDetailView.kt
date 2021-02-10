package org.rfcx.companion.view.map

import java.util.*
import org.rfcx.companion.R
import org.rfcx.companion.entity.EdgeDeployment
import org.rfcx.companion.entity.SyncState
import org.rfcx.companion.entity.guardian.GuardianDeployment

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
        val deployedAt: Date = Date(),
        val state: Int,
        val createdAt: Date,
        val syncState: Int,
        val updatedAt: Date?,
        val deletedAt: Date?,
        override val locationName: String,
        override val latitude: Double,
        override val longitude: Double,
        override val viewType: Int = DeploymentViewPagerAdapter.DEPLOYMENT_EDGE_VIEW
    ) : DeploymentDetailView() {
        val syncImage = when (syncState) {
            SyncState.Unsent.key -> R.drawable.ic_cloud_queue_16dp
            SyncState.Sending.key -> R.drawable.ic_cloud_upload_16dp
            else -> R.drawable.ic_cloud_done_16dp
        }
    }

    data class GuardianDeploymentView(
        override val id: Int,
        var serverId: String? = null,
        val deployedAt: Date,
        val state: Int,
        val wifiName: String?,
        val createdAt: Date,
        val updatedAt: Date?,
        val syncState: Int,
        override val locationName: String,
        override val latitude: Double,
        override val longitude: Double,
        override val viewType: Int = DeploymentViewPagerAdapter.DEPLOYMENT_GUARDIAN_VIEW
    ) : DeploymentDetailView() {
        val syncImage = when (syncState) {
            SyncState.Unsent.key -> R.drawable.ic_cloud_queue_16dp
            SyncState.Sending.key -> R.drawable.ic_cloud_upload_16dp
            else -> R.drawable.ic_cloud_done_16dp
        }
    }
}

fun EdgeDeployment.toEdgeDeploymentView(): DeploymentDetailView.EdgeDeploymentView {
    return DeploymentDetailView.EdgeDeploymentView(
        id = this.id,
        serverId = this.serverId,
        deploymentId = this.deploymentKey ?: "",
        deployedAt = this.deployedAt,
        state = this.state,
        createdAt = this.createdAt,
        syncState = this.syncState,
        updatedAt = this.updatedAt,
        deletedAt = this.deletedAt,
        locationName = this.stream?.name ?: "",
        latitude = this.stream?.latitude ?: 0.0,
        longitude = this.stream?.longitude ?: 0.0
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
        updatedAt = this.updatedAt,
        syncState = this.syncState,
        locationName = this.stream?.name ?: "",
        latitude = this.stream?.latitude ?: 0.0,
        longitude = this.stream?.longitude ?: 0.0
    )
}
