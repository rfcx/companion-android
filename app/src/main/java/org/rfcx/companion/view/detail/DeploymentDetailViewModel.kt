package org.rfcx.companion.view.detail

import android.app.Application
import androidx.lifecycle.AndroidViewModel
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

}
