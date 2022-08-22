package org.rfcx.companion.view.unsynced

import io.realm.RealmResults
import org.rfcx.companion.entity.guardian.Deployment
import org.rfcx.companion.entity.guardian.GuardianRegistration
import org.rfcx.companion.repo.api.DeviceApiHelper
import org.rfcx.companion.repo.local.LocalDataHelper

class UnsyncedWorksRepository(
    private val deviceApiHelper: DeviceApiHelper,
    private val localDataHelper: LocalDataHelper
) {
    fun getUnsentDeployment(): List<Deployment> {
        return localDataHelper.getDeploymentLocalDb().getUnsent()
    }

    fun getUnsetRegistration(): List<GuardianRegistration> {
        return localDataHelper.getGuardianRegistration().getAll()
    }

    fun getAllDeploymentLocalResultsAsync(): RealmResults<Deployment> {
        return localDataHelper.getDeploymentLocalDb().getAllResultsAsync()
    }

    fun getAllRegistrationLocalResultsAsync(): RealmResults<GuardianRegistration> {
        return localDataHelper.getGuardianRegistration().getAllResultsAsync()
    }

    fun deleteDeployment(id: Int) {
        localDataHelper.getDeploymentLocalDb().deleteDeployment(id)
    }

    fun deleteRegistration(id: String) {
        localDataHelper.getGuardianRegistration().delete(id)
    }
}
