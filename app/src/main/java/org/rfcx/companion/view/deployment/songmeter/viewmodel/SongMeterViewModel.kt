package org.rfcx.companion.view.deployment.songmeter.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import org.rfcx.companion.entity.Deployment
import org.rfcx.companion.entity.Locate
import org.rfcx.companion.entity.Project
import org.rfcx.companion.entity.guardian.GuardianDeployment
import org.rfcx.companion.view.deployment.songmeter.repository.SongMeterRepository

class SongMeterViewModel(
    application: Application,
    private val songMeterRepository: SongMeterRepository
) : AndroidViewModel(application) {

    fun getDeploymentsFromLocal(): List<Deployment> {
        return songMeterRepository.getDeploymentFromLocal()
    }

    fun getGuardianDeploymentsFromLocal(): List<GuardianDeployment> {
        return songMeterRepository.getGuardianDeploymentFromLocal()
    }

    fun getLocatesFromLocal(): List<Locate> {
        return songMeterRepository.getLocateFromLocal()
    }

    fun getProjectByName(name: String): Project? {
        return songMeterRepository.getProjectByName(name)
    }

    fun setLocateInsertOrUpdate(locate: Locate) {
        songMeterRepository.setLocateInsertOrUpdate(locate)
    }

    fun updateDeployment(deployment: Deployment) {
        songMeterRepository.updateDeployment(deployment)
    }

    fun deleteImages(deployment: Deployment) {
        songMeterRepository.deleteImages(deployment)
    }

    fun insertImage(
        deployment: Deployment? = null,
        guardianDeployment: GuardianDeployment? = null,
        attachImages: List<String>
    ) {
        songMeterRepository.insertImage(deployment, guardianDeployment, attachImages)
    }

    fun scanBle(isEnabled: Boolean) {
        songMeterRepository.scanBle(isEnabled)
    }

    fun stopBle() {
        songMeterRepository.stopBle()
    }

    fun observeAdvertisement() = songMeterRepository.observeAdvertisement()
}
