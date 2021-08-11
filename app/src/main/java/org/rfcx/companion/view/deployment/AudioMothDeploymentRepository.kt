package org.rfcx.companion.view.deployment

import io.realm.RealmResults
import org.rfcx.companion.entity.Locate
import org.rfcx.companion.repo.api.DeviceApiHelper
import org.rfcx.companion.repo.local.LocalDataHelper

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

}
