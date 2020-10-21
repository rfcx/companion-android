package org.rfcx.companion

import org.rfcx.companion.view.map.DeploymentDetailView

interface DeploymentListener {
    fun getShowDeployments(): List<DeploymentDetailView>
    fun setShowDeployments(deployments: List<DeploymentDetailView>)
}
