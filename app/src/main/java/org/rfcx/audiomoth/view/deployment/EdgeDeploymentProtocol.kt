package org.rfcx.audiomoth.view.deployment

import java.sql.Timestamp
import org.rfcx.audiomoth.entity.*

interface EdgeDeploymentProtocol : BaseDeploymentProtocol {
    fun openWithEdgeDevice()
    fun openWithGuardianDevice()

    fun startSyncing(status: String)

    fun getDeployment(): EdgeDeployment?

    fun setDeployment(deployment: EdgeDeployment)

    fun playSyncSound()
}
