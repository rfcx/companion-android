package org.rfcx.companion.base

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import org.rfcx.companion.MainRepository
import org.rfcx.companion.MainViewModel
import org.rfcx.companion.repo.api.CoreApiHelper
import org.rfcx.companion.repo.api.DeviceApiHelper
import org.rfcx.companion.repo.local.LocalDataHelper
import org.rfcx.companion.view.LoginRepository
import org.rfcx.companion.view.LoginViewModel
import org.rfcx.companion.view.deployment.AudioMothDeploymentRepository
import org.rfcx.companion.view.deployment.AudioMothDeploymentViewModel
import org.rfcx.companion.view.detail.DeploymentDetailRepository
import org.rfcx.companion.view.detail.DeploymentDetailViewModel
import org.rfcx.companion.view.detail.EditLocationRepository
import org.rfcx.companion.view.detail.EditLocationViewModel
import org.rfcx.companion.view.profile.classifier.repository.GuardianClassifierRepository
import org.rfcx.companion.view.profile.classifier.viewmodel.GuardianClassifierViewModel
import org.rfcx.companion.view.profile.guardianregistration.RegisterGuardianRepository
import org.rfcx.companion.view.profile.guardianregistration.RegisterGuardianViewModel
import org.rfcx.companion.view.profile.guardiansoftware.repository.GuardianSoftwareRepository
import org.rfcx.companion.view.profile.guardiansoftware.viewmodel.GuardianSoftwareViewModel
import org.rfcx.companion.view.profile.offlinemap.ProjectOfflineMapRepository
import org.rfcx.companion.view.profile.offlinemap.ProjectOfflineMapViewModel
import org.rfcx.companion.view.project.repository.ProjectSelectRepository
import org.rfcx.companion.view.project.viewmodel.ProjectSelectViewModel
import org.rfcx.companion.view.unsynced.UnsyncedDeploymentRepository
import org.rfcx.companion.view.unsynced.UnsyncedDeploymentViewModel

class ViewModelFactory(
    private val application: Application,
    private val deviceApiHelper: DeviceApiHelper,
    private val coreApiHelper: CoreApiHelper,
    private val localDataHelper: LocalDataHelper
) : ViewModelProvider.Factory {
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        when {
            modelClass.isAssignableFrom(ProjectSelectViewModel::class.java) -> {
                return ProjectSelectViewModel(
                    application,
                    ProjectSelectRepository(deviceApiHelper, localDataHelper)
                ) as T
            }
            modelClass.isAssignableFrom(MainViewModel::class.java) -> {
                return MainViewModel(
                    application,
                    MainRepository(deviceApiHelper, localDataHelper)
                ) as T
            }
            modelClass.isAssignableFrom(ProjectOfflineMapViewModel::class.java) -> {
                return ProjectOfflineMapViewModel(
                    application,
                    ProjectOfflineMapRepository(deviceApiHelper, localDataHelper)
                ) as T
            }
            modelClass.isAssignableFrom(GuardianSoftwareViewModel::class.java) -> {
                return GuardianSoftwareViewModel(
                    application,
                    GuardianSoftwareRepository(coreApiHelper)
                ) as T
            }
            modelClass.isAssignableFrom(LoginViewModel::class.java) -> {
                return LoginViewModel(
                    application,
                    LoginRepository(deviceApiHelper, localDataHelper)
                ) as T
            }
            modelClass.isAssignableFrom(AudioMothDeploymentViewModel::class.java) -> {
                return AudioMothDeploymentViewModel(
                    application,
                    AudioMothDeploymentRepository(deviceApiHelper, localDataHelper)
                ) as T
            }
            modelClass.isAssignableFrom(DeploymentDetailViewModel::class.java) -> {
                return DeploymentDetailViewModel(
                    application,
                    DeploymentDetailRepository(deviceApiHelper, localDataHelper)
                ) as T
            }
            modelClass.isAssignableFrom(UnsyncedDeploymentViewModel::class.java) -> {
                return UnsyncedDeploymentViewModel(
                    application,
                    UnsyncedDeploymentRepository(deviceApiHelper, localDataHelper)
                ) as T
            }
            modelClass.isAssignableFrom(EditLocationViewModel::class.java) -> {
                return EditLocationViewModel(
                    application,
                    EditLocationRepository(deviceApiHelper, localDataHelper)
                ) as T
            }
            modelClass.isAssignableFrom(GuardianClassifierViewModel::class.java) -> {
                return GuardianClassifierViewModel(
                    application,
                    GuardianClassifierRepository(deviceApiHelper, localDataHelper)
                ) as T
            }
            modelClass.isAssignableFrom(RegisterGuardianViewModel::class.java) -> {
                return RegisterGuardianViewModel(
                    application,
                    RegisterGuardianRepository(deviceApiHelper, localDataHelper)
                ) as T
            }
            else -> throw IllegalArgumentException("Unknown class name")
        }
    }
}
