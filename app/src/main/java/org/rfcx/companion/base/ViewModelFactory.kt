package org.rfcx.companion.base

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import org.rfcx.companion.repo.api.DeviceApiHelper
import org.rfcx.companion.repo.ble.BleHelper
import org.rfcx.companion.repo.local.LocalDataHelper
import org.rfcx.companion.view.deployment.songmeter.repository.SongMeterRepository
import org.rfcx.companion.view.deployment.songmeter.viewmodel.SongMeterViewModel
import org.rfcx.companion.view.project.repository.ProjectSelectRepository
import org.rfcx.companion.view.project.viewmodel.ProjectSelectViewModel

class ViewModelFactory(
    private val application: Application,
    private val deviceApiHelper: DeviceApiHelper,
    private val localDataHelper: LocalDataHelper,
    private val bleHelper: BleHelper? = null
) : ViewModelProvider.Factory {
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ProjectSelectViewModel::class.java)) {
            return ProjectSelectViewModel(
                application,
                ProjectSelectRepository(deviceApiHelper, localDataHelper)
            ) as T
        } else if (modelClass.isAssignableFrom(SongMeterViewModel::class.java)) {
            return SongMeterViewModel(
                application,
                SongMeterRepository(deviceApiHelper, localDataHelper, bleHelper!!)
            ) as T
        }
        throw IllegalArgumentException("Unknown class name")
    }
}