package org.rfcx.companion.view.deployment

import io.realm.RealmResults
import org.rfcx.companion.entity.*
import org.rfcx.companion.entity.guardian.Deployment
import org.rfcx.companion.repo.api.DeviceApiHelper
import org.rfcx.companion.repo.local.LocalDataHelper
import java.util.*

class AudioMothDeploymentRepository(
    private val deviceApiHelper: DeviceApiHelper,
    private val localDataHelper: LocalDataHelper
) {

    fun getAllResultsAsyncWithinProject(projectName: String): RealmResults<Locate> {
        return localDataHelper.getLocateLocalDb()
            .getAllResultsAsyncWithinProject(project = projectName)
    }

    fun insertOrUpdate(locate: Locate) {
        localDataHelper.getLocateLocalDb().insertOrUpdate(locate)
    }

    fun insertOrUpdateLocate(deploymentId: Int, locate: Locate) {
        localDataHelper.getLocateLocalDb().insertOrUpdateLocate(deploymentId, locate)
    }

    fun getLocateById(id: Int): Locate? {
        return localDataHelper.getLocateLocalDb().getLocateById(id)
    }

    fun getProjectById(id: Int): Project? {
        return localDataHelper.getProjectLocalDb().getProjectById(id)
    }

    fun getProjectByName(name: String): Project? {
        return localDataHelper.getProjectLocalDb().getProjectByName(name)
    }

    fun deleteImages(id: Int) {
        localDataHelper.getDeploymentImageLocalDb().deleteImages(id)
    }

    fun getImageByDeploymentId(id: Int): List<DeploymentImage> {
        return localDataHelper.getDeploymentImageLocalDb().getImageByDeploymentId(id)
    }

    fun insertImage(
        deployment: Deployment? = null,
        attachImages: List<String>
    ) {
        localDataHelper.getDeploymentImageLocalDb().insertImage(deployment, attachImages)
    }

    fun getFirstTracking(): Tracking? {
        return localDataHelper.getTrackingLocalDb().getFirstTracking()
    }

    fun insertOrUpdateTrackingFile(file: TrackingFile) {
        localDataHelper.getTrackingFileLocalDb().insertOrUpdate(file)
    }

    fun getAllDeploymentResultsAsyncWithinProject(projectName: String): RealmResults<Deployment> {
        return localDataHelper.getDeploymentLocalDb()
            .getAllResultsAsyncWithinProject(project = projectName)
    }

    fun updateDeployment(deployment: Deployment) {
        localDataHelper.getDeploymentLocalDb().updateDeployment(deployment)
    }

    fun insertOrUpdateDeployment(deployment: Deployment, location: DeploymentLocation): Int {
        return localDataHelper.getDeploymentLocalDb()
            .insertOrUpdateDeployment(deployment, location)
    }

    fun getDeploymentsBySiteId(streamId: String): ArrayList<Deployment> {
        return localDataHelper.getDeploymentLocalDb().getDeploymentsBySiteId(streamId)
    }

    fun updateIsActive(id: Int) {
        localDataHelper.getDeploymentLocalDb().updateIsActive(id)
    }

    fun getDeploymentById(id: Int): Deployment? {
        return localDataHelper.getDeploymentLocalDb().getDeploymentById(id)
    }

}
