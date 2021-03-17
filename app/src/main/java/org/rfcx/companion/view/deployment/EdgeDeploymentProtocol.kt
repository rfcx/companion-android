package org.rfcx.companion.view.deployment

import org.rfcx.companion.entity.*

interface EdgeDeploymentProtocol : BaseDeploymentProtocol {
    fun openWithEdgeDevice()
    fun openWithGuardianDevice()

    fun startSyncing(status: String)

    fun getDeployment(): EdgeDeployment?

    fun setDeployment(deployment: EdgeDeployment)

    fun showSyncInstruction()
    fun showSiteLoadingDialog()

    fun playSyncSound()

    fun playTone(duration: Int = 10000)

    fun stopPlaySound()

    fun isSiteLoading(): Boolean
}
