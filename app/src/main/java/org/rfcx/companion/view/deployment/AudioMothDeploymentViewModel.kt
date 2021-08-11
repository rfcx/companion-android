package org.rfcx.companion.view.deployment

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import io.realm.RealmResults
import org.rfcx.companion.entity.*
import org.rfcx.companion.entity.guardian.Deployment
import java.util.*

class AudioMothDeploymentViewModel(
    application: Application,
    private val audioMothDeploymentRepository: AudioMothDeploymentRepository
) : AndroidViewModel(application) {

    fun getAllResultsAsyncWithinProject(projectName: String): RealmResults<Locate> {
        return audioMothDeploymentRepository.getAllResultsAsyncWithinProject(projectName)
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

    fun getAllDeploymentResultsAsyncWithinProject(projectName: String): RealmResults<Deployment> {
        return audioMothDeploymentRepository.getAllDeploymentResultsAsyncWithinProject(projectName)
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
}
