package org.rfcx.companion.view.deployment

import android.annotation.SuppressLint
import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.Transformations
import org.rfcx.companion.R
import org.rfcx.companion.entity.*
import org.rfcx.companion.entity.guardian.Deployment
import org.rfcx.companion.util.Preferences
import org.rfcx.companion.util.asLiveData
import java.util.*

class AudioMothDeploymentViewModel(
    application: Application,
    private val audioMothDeploymentRepository: AudioMothDeploymentRepository
) : AndroidViewModel(application) {

    @SuppressLint("StaticFieldLeak")
    private val context = getApplication<Application>().applicationContext

    private var deployments = MutableLiveData<List<Deployment>>()
    private var sites = MutableLiveData<List<Locate>>()

    private lateinit var deploymentLiveData: LiveData<List<Deployment>>
    private val deploymentObserve = Observer<List<Deployment>> {
        deployments.postValue(it)
    }

    private lateinit var siteLiveData: LiveData<List<Locate>>
    private val siteObserve = Observer<List<Locate>> {
        sites.postValue(it)
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
                audioMothDeploymentRepository.getAllResultsAsyncWithinProject(projectName)
                    .asLiveData()
            ) { it }
        siteLiveData.observeForever(siteObserve)

        deploymentLiveData = Transformations.map(
            audioMothDeploymentRepository.getAllDeploymentResultsAsyncWithinProject(projectName)
                .asLiveData()
        ) { it }
        deploymentLiveData.observeForever(deploymentObserve)
    }

    fun getDeployments(): LiveData<List<Deployment>> {
        return deployments
    }

    fun getSites(): LiveData<List<Locate>> {
        return sites
    }

    fun insertOrUpdate(locate: Locate) {
        audioMothDeploymentRepository.insertOrUpdate(locate)
    }

    fun insertOrUpdateLocate(deploymentId: Int, locate: Locate) {
        audioMothDeploymentRepository.insertOrUpdateLocate(deploymentId, locate)
    }

    fun getProjectById(id: Int): Project? {
        return audioMothDeploymentRepository.getProjectById(id)
    }

    fun getProjectByName(name: String): Project? {
        return audioMothDeploymentRepository.getProjectByName(name)
    }

    fun deleteImages(id: Int) {
        audioMothDeploymentRepository.deleteImages(id)
    }

    fun getImageByDeploymentId(id: Int): List<DeploymentImage> {
        return audioMothDeploymentRepository.getImageByDeploymentId(id)
    }

    fun insertImage(
        deployment: Deployment? = null,
        attachImages: List<String>
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

    fun insertOrUpdateDeployment(deployment: Deployment, location: DeploymentLocation): Int {
        return audioMothDeploymentRepository.insertOrUpdateDeployment(deployment, location)
    }

    fun getDeploymentsBySiteId(streamId: String): ArrayList<Deployment> {
        return audioMothDeploymentRepository.getDeploymentsBySiteId(streamId)
    }

    fun updateIsActive(id: Int) {
        audioMothDeploymentRepository.updateIsActive(id)
    }

    fun getDeploymentById(id: Int): Deployment? {
        return audioMothDeploymentRepository.getDeploymentById(id)
    }

    fun onDestroy() {
        deploymentLiveData.removeObserver(deploymentObserve)
        siteLiveData.removeObserver(siteObserve)
    }
}
