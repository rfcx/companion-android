package org.rfcx.companion.view.detail

import org.rfcx.companion.entity.LocationGroup
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

    fun editProject(id: Int, locationGroup: LocationGroup, callback: DatabaseCallback) {
        return localDataHelper.getDeploymentLocalDb().editProject(id, locationGroup, callback)
    }
}
