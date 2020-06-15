package org.rfcx.audiomoth.view.deployment.guardian

import org.rfcx.audiomoth.view.deployment.BaseDeploymentProtocal

interface GuardianDeploymentProtocol : BaseDeploymentProtocal {
    fun startSetupConfigure()
    fun startSyncing(status: String)
    fun setDeploymentConfigure()
}
