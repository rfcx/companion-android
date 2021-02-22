package org.rfcx.companion.view.deployment

import android.location.Location
import org.rfcx.companion.entity.*

interface EdgeDeploymentProtocol : BaseDeploymentProtocol {
    fun openWithEdgeDevice()
    fun openWithGuardianDevice()

    fun startSyncing(status: String)

    fun getDeployment(): EdgeDeployment?

    fun setDeployment(deployment: EdgeDeployment)

    fun showSyncInstruction()

    fun playSyncSound()

    fun playTone()

    fun stopPlaySound()
}
