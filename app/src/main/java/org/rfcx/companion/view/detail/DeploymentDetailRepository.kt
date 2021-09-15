package org.rfcx.companion.view.detail

import io.realm.RealmResults
import org.rfcx.companion.entity.DeploymentImage
import org.rfcx.companion.entity.Project
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

    fun getAllResultsAsync(deploymentId: Int): RealmResults<DeploymentImage> {
        return localDataHelper.getDeploymentImageLocalDb().getAllResultsAsync(deploymentId)
    }

    fun insertImage(
        deployment: Deployment? = null,
        attachImages: List<String>
    ) {
        localDataHelper.getDeploymentImageLocalDb().insertImage(deployment, attachImages)
    }

    fun isExisted(name: String?): Boolean {
        return localDataHelper.getProjectLocalDb().isExisted(name)
    }

    fun getProjectById(id: Int): Project? {
        return localDataHelper.getProjectLocalDb().getProjectById(id)
    }
}
