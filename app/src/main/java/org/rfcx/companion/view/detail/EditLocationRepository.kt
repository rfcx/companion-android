package org.rfcx.companion.view.detail

import org.rfcx.companion.entity.Project
import org.rfcx.companion.entity.Stream
import org.rfcx.companion.localdb.DatabaseCallback
import org.rfcx.companion.repo.api.DeviceApiHelper
import org.rfcx.companion.repo.local.LocalDataHelper

class EditLocationRepository(
    private val deviceApiHelper: DeviceApiHelper,
    private val localDataHelper: LocalDataHelper
) {

    fun editStream(
        id: Int,
        locationName: String,
        latitude: Double,
        longitude: Double,
        altitude: Double,
        callback: DatabaseCallback
    ) {
        return localDataHelper.getDeploymentLocalDb()
            .editStream(id, locationName, latitude, longitude, altitude, callback)
    }

    fun getStreamById(id: Int): Stream? {
        return localDataHelper.getLocateLocalDb().getLocateById(id)
    }

    fun editProject(id: Int, project: Project, callback: DatabaseCallback) {
        return localDataHelper.getDeploymentLocalDb().editProject(id, project, callback)
    }

    fun getProjectById(id: Int): Project? {
        return localDataHelper.getProjectLocalDb().getProjectById(id)
    }
}
