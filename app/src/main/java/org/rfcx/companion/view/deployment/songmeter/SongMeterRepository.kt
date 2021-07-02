package org.rfcx.companion.view.deployment.songmeter

import org.rfcx.companion.entity.Deployment
import org.rfcx.companion.entity.Locate
import org.rfcx.companion.entity.guardian.GuardianDeployment
import org.rfcx.companion.repo.api.DeviceApiHelper
import org.rfcx.companion.repo.local.LocalDataHelper

class SongMeterRepository(
    private val deviceApiHelper: DeviceApiHelper,
    private val localDataHelper: LocalDataHelper
) {
    fun getDeploymentFromLocal() = localDataHelper.getDeploymentLocalDb().getDeployments()

    fun updateDeployment(deployment: Deployment) =
        localDataHelper.getDeploymentLocalDb().updateDeployment(deployment)

    fun getGuardianDeploymentFromLocal() =
        localDataHelper.getGuardianDeploymentLocalDb().getGuardianDeployments()

    fun getLocateFromLocal() = localDataHelper.getLocateLocalDb().getLocations()

    fun setLocateInsertOrUpdate(locate: Locate) =
        localDataHelper.getLocateLocalDb().insertOrUpdate(locate)

    fun getProjectByName(name: String) = localDataHelper.getProjectLocalDb().getProjectByName(name)

    fun deleteImages(deployment: Deployment) =
        localDataHelper.getDeploymentImageLocalDb().deleteImages(deployment.id)

    fun insertImage(
        deployment: Deployment? = null,
        guardianDeployment: GuardianDeployment? = null,
        attachImages: List<String>
    ) = localDataHelper.getDeploymentImageLocalDb()
        .insertImage(deployment, guardianDeployment, attachImages)

}
