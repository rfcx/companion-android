package org.rfcx.audiomoth

import org.rfcx.audiomoth.entity.Deployment

interface DeploymentListener {
    fun getShowDeployments(): List<Deployment>
    fun setShowDeployments(deployments: List<Deployment>)
}
