package org.rfcx.companion.view.deployment.songmeter

import org.rfcx.companion.entity.Locate
import org.rfcx.companion.repo.api.DeviceApiHelper
import org.rfcx.companion.repo.local.LocalDataHelper

class SongMeterRepository(private val deviceApiHelper: DeviceApiHelper, private val localDataHelper: LocalDataHelper) {
    fun getDeploymentFromLocal() = localDataHelper.getDeploymentLocalDb().getDeployments()
    fun getGuardianDeploymentFromLocal() = localDataHelper.getGuardianDeploymentLocalDb().getGuardianDeployments()
    fun getLocateFromLocal() = localDataHelper.getLocateLocalDb().getLocations()
    fun getProjectByName(name: String) = localDataHelper.getProjectLocalDb().getProjectByName(name)
    fun setLocateInsertOrUpdate(locate: Locate) = localDataHelper.getLocateLocalDb().insertOrUpdate(locate)
}
