package org.rfcx.companion

import android.content.Context
import io.realm.RealmResults
import org.rfcx.companion.entity.*
import org.rfcx.companion.entity.guardian.Deployment
import org.rfcx.companion.entity.response.DeploymentAssetResponse
import org.rfcx.companion.entity.response.ProjectByIdResponse
import org.rfcx.companion.entity.response.ProjectResponse
import org.rfcx.companion.localdb.ProjectDb
import org.rfcx.companion.repo.api.DeviceApiHelper
import org.rfcx.companion.repo.local.LocalDataHelper

class MainRepository(
    private val deviceApiHelper: DeviceApiHelper,
    private val localDataHelper: LocalDataHelper
) {
    fun getProjectsFromRemote(
        limit: Int = 100,
        offset: Int = 0,
        fields: List<String> = listOf("id", "name", "permissions")
    ) =
        deviceApiHelper.getProjects(limit, offset, fields)

    fun getProjectsByIdFromCore(id: String) = deviceApiHelper.getProjectsById(id)

    fun getDeletedProjectsFromRemote(
        limit: Int = 100,
        offset: Int = 0,
        onlyDeleted: Boolean = true,
        fields: List<String> = listOf("id")
    ) = deviceApiHelper.getDeletedProjects(limit, offset, onlyDeleted, fields)

    fun getStreamAssets(id: String) = deviceApiHelper.getStreamAssets(id)

    fun getProjectsFromLocal() = localDataHelper.getProjectLocalDb().getProjects()

    fun saveTrackingToLocal(
        deploymentAssetResponse: DeploymentAssetResponse,
        filePath: String,
        site: Stream?
    ) = localDataHelper.getTrackingFileLocalDb()
        .insertOrUpdate(deploymentAssetResponse, filePath, site)

    fun getAllLocateResultsAsync(): RealmResults<Stream> {
        return localDataHelper.getStreamLocalDb().getAllResultsAsync()
    }

    fun getAllDeploymentLocateResultsAsync(): RealmResults<Deployment> {
        return localDataHelper.getDeploymentLocalDb().getAllResultsAsync()
    }
    fun saveProjectToLocal(projectResponse: ProjectResponse) {
        localDataHelper.getProjectLocalDb().insertOrUpdate(projectResponse)
    }

    fun removeProjectFromLocal(projectResponse: List<ProjectResponse>) {
        localDataHelper.getProjectLocalDb().deleteProjectsByCoreId(projectResponse)
    }

    fun getProjectById(id: Int): Project? {
        return localDataHelper.getProjectLocalDb().getProjectById(id)
    }

    fun getProjectByServerId(id: String): Project? {
        return localDataHelper.getProjectLocalDb().getProjectByServerId(id)
    }

    fun getDeploymentUnsentCount(): Int {
        return localDataHelper.getDeploymentLocalDb().unsentCount().toInt()
    }

    fun getDeploymentById(id: Int): Deployment? {
        return localDataHelper.getDeploymentLocalDb().getDeploymentById(id)
    }

    fun getStreamById(id: Int): Stream? {
        return localDataHelper.getStreamLocalDb().getStreamById(id)
    }

    fun updateOfflineState(state: String, id: String) {
        localDataHelper.getProjectLocalDb().updateOfflineState(state, id)
    }

    fun updateProjectBounds(response: ProjectByIdResponse) {
        localDataHelper.getProjectLocalDb().updateProjectBounds(response)
    }

    fun getProjectLocalDb(): ProjectDb {
        return localDataHelper.getProjectLocalDb()
    }

    fun getTrackingFileBySiteId(id: Int): RealmResults<TrackingFile> {
        return localDataHelper.getTrackingFileLocalDb().getTrackingFileBySiteId(id)
    }

    fun getFirstTracking(): Tracking? {
        return localDataHelper.getTrackingLocalDb().getFirstTracking()
    }

    fun deleteTracking(id: Int, context: Context) {
        localDataHelper.getTrackingLocalDb().deleteTracking(id, context)
    }
}
