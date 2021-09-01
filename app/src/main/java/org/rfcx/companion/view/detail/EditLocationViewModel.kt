package org.rfcx.companion.view.detail

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import org.rfcx.companion.entity.LocationGroup
import org.rfcx.companion.localdb.DatabaseCallback

class EditLocationViewModel(
    application: Application,
    private val editLocationRepository: EditLocationRepository
) : AndroidViewModel(application) {

    fun editStream(
        id: Int,
        locationName: String,
        latitude: Double,
        longitude: Double,
        altitude: Double,
        callback: DatabaseCallback
    ) {
        return editLocationRepository.editStream(
            id,
            locationName,
            latitude,
            longitude,
            altitude,
            callback
        )
    }

    fun editProject(id: Int, locationGroup: LocationGroup, callback: DatabaseCallback) {
        return editLocationRepository.editProject(id, locationGroup, callback)
    }
}
