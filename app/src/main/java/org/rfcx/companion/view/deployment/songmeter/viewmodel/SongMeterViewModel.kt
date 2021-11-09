package org.rfcx.companion.view.deployment.songmeter.viewmodel

import android.annotation.SuppressLint
import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.Transformations
import androidx.work.WorkInfo
import org.rfcx.companion.R
import org.rfcx.companion.entity.*
import org.rfcx.companion.entity.guardian.Deployment
import org.rfcx.companion.service.DownloadStreamsWorker
import org.rfcx.companion.util.AudioMothChimeConnector
import org.rfcx.companion.util.Preferences
import org.rfcx.companion.util.Resource
import org.rfcx.companion.util.asLiveData
import org.rfcx.companion.view.deployment.songmeter.repository.SongMeterRepository
import org.rfcx.companion.view.map.SyncInfo
import java.util.ArrayList

class SongMeterViewModel(
    application: Application,
    private val songMeterRepository: SongMeterRepository
) : AndroidViewModel(application) {

    @SuppressLint("StaticFieldLeak")
    private val context = getApplication<Application>().applicationContext
    private var audioMothConnector = AudioMothChimeConnector()

    private var deployments = MutableLiveData<List<Deployment>>()
    private var sites = MutableLiveData<List<Locate>>()
    private var downloadStreamsWork = MutableLiveData<Resource<SyncInfo>>()

    private lateinit var deploymentLiveData: LiveData<List<Deployment>>
    private val deploymentObserve = Observer<List<Deployment>> {
        deployments.postValue(it)
    }

    private lateinit var siteLiveData: LiveData<List<Locate>>
    private val siteObserve = Observer<List<Locate>> {
        sites.postValue(it)
    }

    private lateinit var downloadStreamsWorkInfoLiveData: LiveData<List<WorkInfo>>
    private val downloadStreamsWorkInfoObserve = Observer<List<WorkInfo>> {
        val currentWorkStatus = it?.getOrNull(0)
        if (currentWorkStatus != null) {
            when (currentWorkStatus.state) {
                WorkInfo.State.RUNNING -> downloadStreamsWork.postValue(Resource.loading(SyncInfo.Uploading))
                WorkInfo.State.SUCCEEDED -> downloadStreamsWork.postValue(Resource.success(SyncInfo.Uploaded))
                else -> downloadStreamsWork.postValue(
                    Resource.error(
                        context.getString(R.string.error_has_occurred),
                        null
                    )
                )

            }
        }
    }

    init {
        fetchLiveData()
    }

    private fun fetchLiveData() {
        val preferences = Preferences.getInstance(context)
        val projectId = preferences.getInt(Preferences.SELECTED_PROJECT)
        val project = getProjectById(projectId)
        val projectName = project?.name ?: context.getString(R.string.none)
        siteLiveData =
            Transformations.map(
                songMeterRepository.getAllResultsAsyncWithinProject(projectName)
                    .asLiveData()
            ) { it }
        siteLiveData.observeForever(siteObserve)

        deploymentLiveData = Transformations.map(
            songMeterRepository.getAllDeploymentResultsAsyncWithinProject(projectName)
                .asLiveData()
        ) { it }
        deploymentLiveData.observeForever(deploymentObserve)

        downloadStreamsWorkInfoLiveData = DownloadStreamsWorker.workInfos(context)
        downloadStreamsWorkInfoLiveData.observeForever(downloadStreamsWorkInfoObserve)

    }

    fun getDeployments(): LiveData<List<Deployment>> {
        return deployments
    }

    fun getSites(): LiveData<List<Locate>> {
        return sites
    }

    fun getDeploymentsFromLocal(): List<Deployment> {
        return songMeterRepository.getDeploymentFromLocal()
    }

    fun getLocatesFromLocal(): List<Locate> {
        return songMeterRepository.getLocateFromLocal()
    }

    fun getProjectById(id: Int): Project? {
        return songMeterRepository.getProjectById(id)
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
        attachImages: List<String>
    ) {
        songMeterRepository.insertImage(deployment, attachImages)
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

    fun getSetSiteLiveData() = songMeterRepository.getSetSiteLiveData()

    fun getRequestConfigLiveData() = songMeterRepository.getRequestConfigLiveData()

    fun setPrefixes(prefixes: String) {
        songMeterRepository.setPrefixes(prefixes)
    }

    fun insertOrUpdateDeployment(deployment: Deployment, deploymentLocation: DeploymentLocation): Int {
        return songMeterRepository.insertOrUpdateDeployment(deployment, deploymentLocation)
    }

    fun insetOrUpdateStream(deploymentId: Int, stream: Locate) {
        songMeterRepository.insertOrUpdateStream(deploymentId, stream)
    }

    fun getDeploymentsBySiteId(streamId: String): ArrayList<Deployment> {
        return songMeterRepository.getDeploymentsBySiteId(streamId)
    }

    fun updateIsActive(id: Int) {
        songMeterRepository.updateIsActive(id)
    }

    fun getFirstTracking(): Tracking? {
        return songMeterRepository.getFirstTracking()
    }

    fun insertOrUpdateTrackingFile(file: TrackingFile) {
        songMeterRepository.insertOrUpdateTrackingFile(file)
    }

}
