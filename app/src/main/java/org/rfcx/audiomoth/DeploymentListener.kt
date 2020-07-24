package org.rfcx.audiomoth

import org.rfcx.audiomoth.view.map.DeploymentBottomSheet

interface DeploymentListener {
    fun getShowDeployments(): List<DeploymentBottomSheet>
    fun setShowDeployments(deployments: List<DeploymentBottomSheet>)
}
