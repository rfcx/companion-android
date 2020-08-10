package org.rfcx.audiomoth

import org.rfcx.audiomoth.view.map.DeploymentDetailView

interface DeploymentListener {
    fun getShowDeployments(): List<DeploymentDetailView>
    fun setShowDeployments(deployments: List<DeploymentDetailView>, deploymentId : Int)
}
