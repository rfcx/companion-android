package org.rfcx.companion.view.deployment.songmeter.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import org.rfcx.companion.entity.*
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

    fun observeGattConnection() = songMeterRepository.observeGattConnection()

    fun registerGattReceiver() {
        songMeterRepository.registerGattReceiver()
    }

    fun unRegisterGattReceiver() {
        songMeterRepository.unRegisterGattReceiver()
    }

    fun bindConnectService(address: String) {
        songMeterRepository.bindConnectService(address)
    }

    fun unBindConnectService() {
        songMeterRepository.unBindConnectService()
    }

    fun insertOrUpdateDeployment(deployment: Deployment, deploymentLocation: DeploymentLocation): Int {
        return 0
    }

    fun insetOrUpdateStream(deploymentId: Int, stream: Locate) {

    }

    fun getFirstTracking(): Tracking? {
        return null
    }

    fun insertOrUpdateTrackingFile(file: TrackingFile) {

    }

}
