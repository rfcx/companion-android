package org.rfcx.companion.base

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import org.rfcx.companion.MainRepository
import org.rfcx.companion.MainViewModel
import org.rfcx.companion.repo.api.DeviceApiHelper
import org.rfcx.companion.repo.local.LocalDataHelper
import org.rfcx.companion.view.deployment.AudioMothDeploymentRepository
import org.rfcx.companion.view.deployment.AudioMothDeploymentViewModel
import org.rfcx.companion.view.profile.offlinemap.OfflineMapActivity
import org.rfcx.companion.view.profile.offlinemap.ProjectOfflineMapRepository
import org.rfcx.companion.view.profile.offlinemap.ProjectOfflineMapViewModel
import org.rfcx.companion.view.project.repository.ProjectSelectRepository
import org.rfcx.companion.view.project.viewmodel.ProjectSelectViewModel

class ViewModelFactory(private val application: Application, private val deviceApiHelper: DeviceApiHelper, private val localDataHelper: LocalDataHelper): ViewModelProvider.Factory {
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ProjectSelectViewModel::class.java)) {
            return ProjectSelectViewModel(application, ProjectSelectRepository(deviceApiHelper, localDataHelper)) as T
        } else if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
            return MainViewModel(application, MainRepository(deviceApiHelper, localDataHelper)) as T
        } else if (modelClass.isAssignableFrom(ProjectOfflineMapViewModel::class.java)) {
            return ProjectOfflineMapViewModel(application, ProjectOfflineMapRepository(deviceApiHelper, localDataHelper)) as T
        } else if (modelClass.isAssignableFrom(AudioMothDeploymentViewModel::class.java)) {
            return AudioMothDeploymentViewModel(application, AudioMothDeploymentRepository(deviceApiHelper, localDataHelper)) as T
        }
        throw IllegalArgumentException("Unknown class name")
    }
}
