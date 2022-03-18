package org.rfcx.companion.view.profile.offlinemap

import io.realm.RealmResults
import org.rfcx.companion.entity.Project
import org.rfcx.companion.repo.api.DeviceApiHelper
import org.rfcx.companion.repo.local.LocalDataHelper

class ProjectOfflineMapRepository(
    private val deviceApiHelper: DeviceApiHelper,
    private val localDataHelper: LocalDataHelper
) {

    fun getAllProjectResultsAsync(): RealmResults<Project> {
        return localDataHelper.getProjectLocalDb().getAllResultsAsync()
    }

    fun getOfflineDownloading(): Project? {
        return localDataHelper.getProjectLocalDb().getOfflineDownloading()
    }

    fun getProjectsFromLocal(): List<Project> {
        return localDataHelper.getProjectLocalDb().getProjects()
    }

    fun updateOfflineDownloadedState() {
        localDataHelper.getProjectLocalDb().updateOfflineDownloadedState()
    }

    fun updateOfflineState(state: String, id: String) {
        localDataHelper.getProjectLocalDb().updateOfflineState(state, id)
    }
}
