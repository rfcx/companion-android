package org.rfcx.companion.view.deployment

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import io.realm.RealmResults
import org.rfcx.companion.entity.Locate

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
}
