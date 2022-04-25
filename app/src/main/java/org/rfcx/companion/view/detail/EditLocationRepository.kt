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

    fun markDeploymentNeedUpdate(id: Int) {
        return localDataHelper.getDeploymentLocalDb().markNeedUpdate(id)
    }

    fun editStream(
        id: Int,
        locationName: String,
        latitude: Double,
        longitude: Double,
        altitude: Double,
        projectId: Int
    ) {
        return localDataHelper.getStreamLocalDb()
            .updateValues(
                id = id,
                name = locationName,
                latitude = latitude,
                longitude = longitude,
                altitude = altitude,
                projectId = projectId
            )
    }

    fun getStreamById(id: Int): Stream? {
        return localDataHelper.getStreamLocalDb().getStreamById(id)
    }

    fun getProjectById(id: Int): Project? {
        return localDataHelper.getProjectLocalDb().getProjectById(id)
    }
}
