package org.rfcx.companion.view.deployment

import org.rfcx.companion.entity.*
import org.rfcx.companion.service.DownloadStreamState

interface EdgeDeploymentProtocol : BaseDeploymentProtocol {
    fun openWithEdgeDevice()
    fun openWithGuardianDevice()

    fun startSyncing(status: String)

    fun getDeployment(): EdgeDeployment?

    fun setDeployment(deployment: EdgeDeployment)

    fun playSyncSound()

    fun playTone(duration: Int = 10000)

    fun stopPlaySound()
}
