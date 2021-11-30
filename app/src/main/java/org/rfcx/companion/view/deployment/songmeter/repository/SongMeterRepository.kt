package org.rfcx.companion.view.deployment.songmeter.repository

import io.realm.RealmResults
import org.rfcx.companion.entity.*
import org.rfcx.companion.entity.guardian.Deployment
import org.rfcx.companion.repo.api.DeviceApiHelper
import org.rfcx.companion.repo.ble.BleHelper
import org.rfcx.companion.repo.local.LocalDataHelper
import java.util.ArrayList

class SongMeterRepository(
    private val deviceApiHelper: DeviceApiHelper,
    private val localDataHelper: LocalDataHelper,
    private val bleHelper: BleHelper
) {
    fun getAllResultsAsyncWithinProject(projectName: String): RealmResults<Locate> {
        return localDataHelper.getLocateLocalDb()
            .getAllResultsAsyncWithinProject(project = projectName)
    }

    fun getAllDeploymentResultsAsyncWithinProject(projectName: String): RealmResults<Deployment> {
        return localDataHelper.getDeploymentLocalDb()
            .getAllResultsAsyncWithinProject(project = projectName)
    }

    fun getDeploymentFromLocal() = localDataHelper.getDeploymentLocalDb().getDeployments()

    fun getLocateFromLocal() = localDataHelper.getLocateLocalDb().getLocations()

    fun setLocateInsertOrUpdate(locate: Locate) =
        localDataHelper.getLocateLocalDb().insertOrUpdate(locate)

    fun getProjectById(id: Int): Project? {
        return localDataHelper.getProjectLocalDb().getProjectById(id)
    }

    fun getProjectByName(name: String) = localDataHelper.getProjectLocalDb().getProjectByName(name)

    fun deleteImages(deployment: Deployment) =
        localDataHelper.getDeploymentImageLocalDb().deleteImages(deployment.id)

    fun insertImage(
        deployment: Deployment? = null,
        attachImages: List<String>
    ) = localDataHelper.getDeploymentImageLocalDb()
        .insertImage(deployment, attachImages)

    fun scanBle(isEnabled: Boolean) {
        bleHelper.scanBle(isEnabled)
    }

    fun stopBle() {
        bleHelper.stopScanBle()
    }

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

    fun updateDeployment(deployment: Deployment) {
        localDataHelper.getDeploymentLocalDb().updateDeployment(deployment)
    }

    fun insertOrUpdateDeployment(deployment: Deployment, location: DeploymentLocation): Int {
        return localDataHelper.getDeploymentLocalDb()
            .insertOrUpdateDeployment(deployment, location)
    }

    fun getDeploymentsBySiteId(streamId: String): ArrayList<Deployment> {
        return localDataHelper.getDeploymentLocalDb().getDeploymentsBySiteId(streamId, Device.SONGMETER.value)
    }

    fun updateIsActive(id: Int) {
        localDataHelper.getDeploymentLocalDb().updateIsActive(id)
    }

    fun insertOrUpdateStream(deploymentId: Int, locate: Locate) {
        localDataHelper.getLocateLocalDb().insertOrUpdateLocate(deploymentId, locate)
    }

    fun insertOrUpdateTrackingFile(file: TrackingFile) {
        localDataHelper.getTrackingFileLocalDb().insertOrUpdate(file)
    }

    fun getFirstTracking(): Tracking? {
        return localDataHelper.getTrackingLocalDb().getFirstTracking()
    }

}
