package org.rfcx.companion.view.deployment

import org.rfcx.companion.entity.*
import org.rfcx.companion.entity.guardian.GuardianDeployment
import org.rfcx.companion.service.DownloadStreamState

interface AudioMothDeploymentProtocol : BaseDeploymentProtocol {
    fun openWithEdgeDevice()
    fun openWithGuardianDevice()

    fun startSyncing(status: String)

    fun getDeployment(): GuardianDeployment?

    fun setDeployment(deployment: GuardianDeployment)

    fun playSyncSound()

    fun playTone(duration: Int = 10000)

    fun stopPlaySound()
}
