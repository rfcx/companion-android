package org.rfcx.companion.view.deployment

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
import org.rfcx.companion.view.map.SyncInfo
import java.util.*

class AudioMothDeploymentViewModel(
    application: Application,
    private val audioMothDeploymentRepository: AudioMothDeploymentRepository
) : AndroidViewModel(application) {

    @SuppressLint("StaticFieldLeak")
    private val context = getApplication<Application>().applicationContext
    private var audioMothConnector = AudioMothChimeConnector()

    private var deployments = MutableLiveData<List<Deployment>>()
    private var sites = MutableLiveData<List<Stream>>()
    private var downloadStreamsWork = MutableLiveData<Resource<SyncInfo>>()

    private lateinit var deploymentLiveData: LiveData<List<Deployment>>
    private val deploymentObserve = Observer<List<Deployment>> {
        deployments.postValue(it)
    }

    private lateinit var siteLiveData: LiveData<List<Stream>>
    private val siteObserve = Observer<List<Stream>> {
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
        siteLiveData =
            Transformations.map(
                audioMothDeploymentRepository.getAllResultsAsyncWithinProject(projectId)
                    .asLiveData()
            ) { it }
        siteLiveData.observeForever(siteObserve)

        deploymentLiveData = Transformations.map(
            audioMothDeploymentRepository.getAllDeploymentResultsAsyncWithinProject(projectId)
                .asLiveData()
        ) { it }
        deploymentLiveData.observeForever(deploymentObserve)

        downloadStreamsWorkInfoLiveData = DownloadStreamsWorker.workInfos(context)
        downloadStreamsWorkInfoLiveData.observeForever(downloadStreamsWorkInfoObserve)
    }

    fun downloadStreamsWork(): LiveData<Resource<SyncInfo>> {
        return downloadStreamsWork
    }

    fun getDeployments(): LiveData<List<Deployment>> {
        return deployments
    }

    fun getSites(): LiveData<List<Stream>> {
        return sites
    }

    fun insertOrUpdate(stream: Stream): Int {
        return audioMothDeploymentRepository.insertOrUpdate(stream)
    }

    fun updateDeploymentIdOnStream(deploymentId: Int, streamId: Int) {
        audioMothDeploymentRepository.updateDeploymentIdOnStream(deploymentId, streamId)
    }

    fun getStreamById(id: Int): Stream? {
        return audioMothDeploymentRepository.getLocateById(id)
    }

    fun getProjectById(id: Int): Project? {
        return audioMothDeploymentRepository.getProjectById(id)
    }

    fun deleteImages(id: Int) {
        audioMothDeploymentRepository.deleteImages(id)
    }

    fun getImageByDeploymentId(id: Int): List<DeploymentImage> {
        return audioMothDeploymentRepository.getImageByDeploymentId(id)
    }

    fun insertImage(
        deployment: Deployment? = null,
        attachImages: List<Image>
    ) {
        audioMothDeploymentRepository.insertImage(deployment, attachImages)
    }

    fun getFirstTracking(): Tracking? {
        return audioMothDeploymentRepository.getFirstTracking()
    }

    fun insertOrUpdateTrackingFile(file: TrackingFile) {
        audioMothDeploymentRepository.insertOrUpdateTrackingFile(file)
    }

    fun updateDeployment(deployment: Deployment) {
        audioMothDeploymentRepository.updateDeployment(deployment)
    }

    fun insertOrUpdateDeployment(deployment: Deployment, streamId: Int): Int {
        return audioMothDeploymentRepository.insertOrUpdateDeployment(deployment, streamId)
    }

    fun updateIsActive(id: Int) {
        audioMothDeploymentRepository.updateIsActive(id)
    }

    fun deleteDeployment(id: Int) {
        audioMothDeploymentRepository.deleteDeployment(id)
    }

    fun getDeploymentById(id: Int): Deployment? {
        return audioMothDeploymentRepository.getDeploymentById(id)
    }

    fun playSyncSound(calendar: Calendar, latitude: Double?, longitude: Double?, deploymentID: Array<Int>) {
        audioMothConnector.playTimeAndDeploymentID(
            calendar,
            latitude,
            longitude,
            deploymentID
        )
    }

    fun playTone(duration: Int) {
        audioMothConnector.playTone(duration)
    }

    fun stopPlaySound() {
        audioMothConnector.stopPlay()
    }

    fun onDestroy() {
        deploymentLiveData.removeObserver(deploymentObserve)
        siteLiveData.removeObserver(siteObserve)
        downloadStreamsWorkInfoLiveData.removeObserver(downloadStreamsWorkInfoObserve)
    }
}
