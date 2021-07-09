package org.rfcx.companion.view.deployment.songmeter

import org.rfcx.companion.entity.Deployment
import org.rfcx.companion.entity.songmeter.Advertisement
import org.rfcx.companion.view.deployment.BaseDeploymentProtocol

interface SongMeterDeploymentProtocol : BaseDeploymentProtocol {
    fun getDeployment(): Deployment?
    fun setDeployment(deployment: Deployment)

    fun redirectToConnectSongMeter(advertisement: Advertisement)
}
