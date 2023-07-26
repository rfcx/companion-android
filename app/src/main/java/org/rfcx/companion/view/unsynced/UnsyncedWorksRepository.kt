package org.rfcx.companion.view.unsynced

import io.realm.RealmResults
import org.rfcx.companion.entity.guardian.Deployment
import org.rfcx.companion.repo.local.LocalDataHelper

class UnsyncedWorksRepository(
    private val localDataHelper: LocalDataHelper
) {
    fun getUnsentDeployment(): List<Deployment> {
        return localDataHelper.getDeploymentLocalDb().getUnsent()
    }

    fun getAllDeploymentLocalResultsAsync(): RealmResults<Deployment> {
        return localDataHelper.getDeploymentLocalDb().getAllResultsAsync()
    }
    fun deleteDeployment(id: Int) {
        localDataHelper.getDeploymentLocalDb().deleteDeployment(id)
    }
}
