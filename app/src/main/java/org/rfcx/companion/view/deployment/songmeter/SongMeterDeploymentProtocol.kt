package org.rfcx.companion.view.deployment.songmeter

import org.rfcx.companion.entity.Deployment
import org.rfcx.companion.view.deployment.BaseDeploymentProtocol

interface SongMeterDeploymentProtocol : BaseDeploymentProtocol {
    fun setSongMeterId(id: String)

    fun getDeployment(): Deployment?
    fun setDeployment(deployment: Deployment)
}
