package org.rfcx.companion.view.unsynced

import io.realm.RealmResults
import org.rfcx.companion.entity.guardian.Deployment
import org.rfcx.companion.repo.api.DeviceApiHelper
import org.rfcx.companion.repo.local.LocalDataHelper

class UnsyncedDeploymentRepository(
    private val deviceApiHelper: DeviceApiHelper,
    private val localDataHelper: LocalDataHelper
) {
    fun getUnsentDeployment(): Long {
        return localDataHelper.getDeploymentLocalDb().unsentCount()
    }

    fun getAllDeploymentLocalResultsAsync(): RealmResults<Deployment> {
        return localDataHelper.getDeploymentLocalDb().getAllResultsAsync()
    }
}
