package org.rfcx.companion.view.deployment

import org.rfcx.companion.entity.Deployment

interface AudioMothDeploymentProtocol : BaseDeploymentProtocol {
    fun openWithEdgeDevice()

    fun startSyncing(status: String)

    fun getDeployment(): Deployment?

    fun setDeployment(deployment: Deployment)

    fun playSyncSound()

    fun playTone(duration: Int = 10000)

    fun stopPlaySound()
}
