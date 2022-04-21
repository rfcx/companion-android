package org.rfcx.companion.view.detail

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import org.rfcx.companion.entity.Project
import org.rfcx.companion.entity.Stream
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

    fun getStreamById(id: Int): Stream? {
        return editLocationRepository.getStreamById(id)
    }

    fun editProject(id: Int, project: Project, callback: DatabaseCallback) {
        return editLocationRepository.editProject(id, project, callback)
    }

    fun getProjectById(id: Int): Project? {
        return editLocationRepository.getProjectById(id)
    }
}
