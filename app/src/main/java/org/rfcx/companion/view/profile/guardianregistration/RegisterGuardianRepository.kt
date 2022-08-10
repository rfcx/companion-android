package org.rfcx.companion.view.profile.guardianregistration

import io.realm.RealmResults
import org.rfcx.companion.entity.guardian.GuardianRegistration
import org.rfcx.companion.repo.api.DeviceApiHelper
import org.rfcx.companion.repo.local.LocalDataHelper

class RegisterGuardianRepository(
    private val deviceApiHelper: DeviceApiHelper,
    private val localDataHelper: LocalDataHelper
) {
    fun getRegistrations(): List<GuardianRegistration>? {
        return localDataHelper.getGuardianRegistration().getAll()
    }

    fun getAllGuardianRegistrationLocalResultsAsync(): RealmResults<GuardianRegistration> {
        return localDataHelper.getGuardianRegistration().getAllResultsAsync()
    }

    fun deleteRegistration(guid: String) {
        localDataHelper.getGuardianRegistration().delete(guid)
    }
}
