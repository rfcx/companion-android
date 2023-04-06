package org.rfcx.companion.view.detail.image

import org.rfcx.companion.entity.DeploymentImage
import org.rfcx.companion.repo.local.LocalDataHelper
import org.rfcx.companion.view.deployment.Image

class AddImageRepository(
    private val localDataHelper: LocalDataHelper
) {

    fun getImages(deploymentId: Int): List<DeploymentImage> {
       return localDataHelper.getDeploymentImageLocalDb().getImageByDeploymentId(deploymentId)
    }

    fun saveImages(images: List<Image>, deploymentId: Int) {
        return localDataHelper.getDeploymentImageLocalDb().insertImage(deploymentId, images)
    }
}
