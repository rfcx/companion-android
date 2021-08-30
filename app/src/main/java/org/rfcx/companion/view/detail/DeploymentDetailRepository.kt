package org.rfcx.companion.view.detail

import org.rfcx.companion.entity.guardian.Deployment
import org.rfcx.companion.localdb.DatabaseCallback
import org.rfcx.companion.repo.api.DeviceApiHelper
import org.rfcx.companion.repo.local.LocalDataHelper

class DeploymentDetailRepository(
    private val deviceApiHelper: DeviceApiHelper,
    private val localDataHelper: LocalDataHelper
) {
    fun getDeploymentById(id: Int): Deployment? {
        return localDataHelper.getDeploymentLocalDb().getDeploymentById(id)
    }

    fun deleteDeploymentLocation(id: Int, callback: DatabaseCallback) {
        return localDataHelper.getDeploymentLocalDb().deleteDeploymentLocation(id, callback)
    }
}
