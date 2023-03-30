package org.rfcx.companion.view.deployment.songmeter.repository

import io.realm.RealmResults
import org.rfcx.companion.entity.*
import org.rfcx.companion.entity.guardian.Deployment
import org.rfcx.companion.repo.api.DeviceApiHelper
import org.rfcx.companion.repo.ble.BleHelper
import org.rfcx.companion.repo.local.LocalDataHelper
import org.rfcx.companion.view.deployment.Image

class SongMeterRepository(
    private val deviceApiHelper: DeviceApiHelper,
    private val localDataHelper: LocalDataHelper,
    private val bleHelper: BleHelper
) {
    fun getAllResultsAsyncWithinProject(id: Int): RealmResults<Stream> {
        return localDataHelper.getStreamLocalDb()
            .getAllResultsAsyncWithinProject(id = id)
    }

    fun getAllDeploymentResultsAsyncWithinProject(id: Int): RealmResults<Deployment> {
        return localDataHelper.getDeploymentLocalDb()
            .getAllResultsAsyncWithinProject(id = id)
    }

    fun getStreamById(id: Int): Stream? {
        return localDataHelper.getStreamLocalDb().getStreamById(id)
    }

    fun getProjectById(id: Int): Project? {
        return localDataHelper.getProjectLocalDb().getProjectById(id)
    }

    fun insertOrUpdate(stream: Stream): Int {
        return localDataHelper.getStreamLocalDb().insertOrUpdate(stream)
    }

    fun updateDeploymentIdOnStream(deploymentId: Int, streamId: Int) {
        localDataHelper.getStreamLocalDb().updateDeploymentIdOnStream(deploymentId, streamId)
    }

    fun getImageByDeploymentId(id: Int): List<DeploymentImage> {
        return localDataHelper.getDeploymentImageLocalDb().getImageByDeploymentId(id)
    }

    fun deleteImages(deployment: Deployment) =
        localDataHelper.getDeploymentImageLocalDb().deleteImages(deployment.id)

    fun insertImage(
        deployment: Deployment? = null,
        attachImages: List<Image>
    ) = localDataHelper.getDeploymentImageLocalDb()
        .insertImage(deployment, attachImages)

    fun isBluetoothEnabled(): Boolean {
        return bleHelper.isBluetoothEnabled()
    }

    fun scanBle(isEnabled: Boolean) {
        bleHelper.scanBle(isEnabled)
    }

    fun stopBle() {
        bleHelper.stopScanBle()
    }

    fun clearAdvertisement() = bleHelper.clearAdvertisement()

    fun observeAdvertisement() = bleHelper.observeAdvertisement()

    fun observeGattConnection() = bleHelper.observeGattConnection()

    fun registerGattReceiver() {
        bleHelper.registerGattReceiver()
    }

    fun unRegisterGattReceiver() {
        bleHelper.unRegisterGattReceiver()
    }

    fun bindConnectService(address: String) {
        bleHelper.bindConnectService(address)
    }

    fun unBindConnectService() {
        bleHelper.unBindConnectService()
    }

    fun getSetSiteLiveData() = bleHelper.getSetSiteLiveData()

    fun getRequestConfigLiveData() = bleHelper.getRequestConfigLiveData()

    fun setPrefixes(prefixes: String) {
        bleHelper.setPrefixes(prefixes)
    }

    fun getDeploymentById(id: Int): Deployment? {
        return localDataHelper.getDeploymentLocalDb().getDeploymentById(id)
    }

    fun updateDeployment(deployment: Deployment) {
        localDataHelper.getDeploymentLocalDb().updateDeployment(deployment)
    }

    fun insertOrUpdateDeployment(deployment: Deployment, streamId: Int): Int {
        return localDataHelper.getDeploymentLocalDb()
            .insertOrUpdateDeployment(deployment, streamId)
    }

    fun updateIsActive(id: Int) {
        localDataHelper.getDeploymentLocalDb().updateIsActive(id)
    }

    fun insertOrUpdateTrackingFile(file: TrackingFile) {
        localDataHelper.getTrackingFileLocalDb().insertOrUpdate(file)
    }

    fun getFirstTracking(): Tracking? {
        return localDataHelper.getTrackingLocalDb().getFirstTracking()
    }
}
