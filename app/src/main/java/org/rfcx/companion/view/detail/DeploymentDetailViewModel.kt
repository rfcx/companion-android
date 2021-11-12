package org.rfcx.companion.view.detail

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import io.realm.RealmResults
import org.rfcx.companion.entity.DeploymentImage
import org.rfcx.companion.entity.Project
import org.rfcx.companion.entity.guardian.Deployment
import org.rfcx.companion.localdb.DatabaseCallback

class DeploymentDetailViewModel(
    application: Application,
    private val deploymentDetailRepository: DeploymentDetailRepository
) : AndroidViewModel(application) {

    fun getDeploymentById(id: Int): Deployment? {
        return deploymentDetailRepository.getDeploymentById(id)
    }

    fun deleteDeploymentLocation(id: Int, callback: DatabaseCallback) {
        return deploymentDetailRepository.deleteDeploymentLocation(id, callback)
    }

    fun getAllResultsAsync(deploymentId: Int, device: String): RealmResults<DeploymentImage> {
        return deploymentDetailRepository.getAllResultsAsync(deploymentId, device)
    }

    fun insertImage(
        deployment: Deployment? = null,
        attachImages: List<String>
    ) {
        deploymentDetailRepository.insertImage(deployment, attachImages)
    }

    fun isExisted(name: String?): Boolean {
        return deploymentDetailRepository.isExisted(name)
    }

    fun getProjectById(id: Int): Project? {
        return deploymentDetailRepository.getProjectById(id)
    }
}
