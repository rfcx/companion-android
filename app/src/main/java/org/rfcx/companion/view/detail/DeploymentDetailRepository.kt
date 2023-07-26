package org.rfcx.companion.view.detail

import io.realm.RealmResults
import org.rfcx.companion.entity.DeploymentImage
import org.rfcx.companion.entity.Project
import org.rfcx.companion.entity.guardian.Deployment
import org.rfcx.companion.localdb.DatabaseCallback
import org.rfcx.companion.repo.api.DeviceApiHelper
import org.rfcx.companion.repo.local.LocalDataHelper
import org.rfcx.companion.view.deployment.Image
import org.rfcx.companion.view.deployment.ImageType

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

    fun getAllResultsAsync(deploymentId: Int, device: String): RealmResults<DeploymentImage> {
        return localDataHelper.getDeploymentImageLocalDb().getAllResultsAsync(deploymentId, device)
    }

    fun getAllDeploymentLocateResultsAsync(): RealmResults<Deployment> {
        return localDataHelper.getDeploymentLocalDb().getAllResultsAsync()
    }

    fun insertImage(
        deployment: Deployment? = null,
        attachImages: List<DeploymentImageView>
    ) {
        localDataHelper.getDeploymentImageLocalDb().insertImage(
            deployment,
            attachImages.map {
                Image(path = it.localPath, name = it.label, type = ImageType.OTHER, id = 0, remotePath = it.remotePath)
            }
        )
    }

    fun isExisted(name: String?): Boolean {
        return localDataHelper.getProjectLocalDb().isExisted(name)
    }

    fun getProjectById(id: Int): Project? {
        return localDataHelper.getProjectLocalDb().getProjectById(id)
    }
}
