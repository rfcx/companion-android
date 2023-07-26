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

    fun getAllResultsAsyncWithinProject(id: Int): RealmResults<Stream> {
        return localDataHelper.getStreamLocalDb()
            .getAllResultsAsyncWithinProject(id = id)
    }

    fun insertOrUpdate(stream: Stream): Int {
        return localDataHelper.getStreamLocalDb().insertOrUpdate(stream)
    }

    fun updateDeploymentIdOnStream(deploymentId: Int, streamId: Int) {
        localDataHelper.getStreamLocalDb().updateDeploymentIdOnStream(deploymentId, streamId)
    }

    fun getLocateById(id: Int): Stream? {
        return localDataHelper.getStreamLocalDb().getStreamById(id)
    }

    fun getProjectById(id: Int): Project? {
        return localDataHelper.getProjectLocalDb().getProjectById(id)
    }

    fun deleteImages(id: Int) {
        localDataHelper.getDeploymentImageLocalDb().deleteImages(id)
    }

    fun getImageByDeploymentId(id: Int): List<DeploymentImage> {
        return localDataHelper.getDeploymentImageLocalDb().getImageByDeploymentId(id)
    }

    fun insertImage(
        deployment: Deployment? = null,
        attachImages: List<Image>
    ) {
        localDataHelper.getDeploymentImageLocalDb().insertImage(deployment, attachImages)
    }

    fun getFirstTracking(): Tracking? {
        return localDataHelper.getTrackingLocalDb().getFirstTracking()
    }

    fun insertOrUpdateTrackingFile(file: TrackingFile) {
        localDataHelper.getTrackingFileLocalDb().insertOrUpdate(file)
    }

    fun getAllDeploymentResultsAsyncWithinProject(id: Int): RealmResults<Deployment> {
        return localDataHelper.getDeploymentLocalDb()
            .getAllResultsAsyncWithinProject(id = id)
    }

    fun updateDeployment(deployment: Deployment) {
        localDataHelper.getDeploymentLocalDb().updateDeployment(deployment)
    }

    fun insertOrUpdateDeployment(deployment: Deployment, streamId: Int): Int {
        return localDataHelper.getDeploymentLocalDb()
            .insertOrUpdateDeployment(deployment, streamId)
    }

    fun getDeploymentsBySiteId(streamId: Int): ArrayList<Deployment> {
        return localDataHelper.getDeploymentLocalDb().getDeploymentsBySiteId(streamId)
    }

    fun updateIsActive(id: Int) {
        localDataHelper.getDeploymentLocalDb().updateIsActive(id)
    }

    fun getDeploymentById(id: Int): Deployment? {
        return localDataHelper.getDeploymentLocalDb().getDeploymentById(id)
    }

    fun deleteDeployment(id: Int) {
        return localDataHelper.getDeploymentLocalDb().deleteDeployment(id)
    }
}
