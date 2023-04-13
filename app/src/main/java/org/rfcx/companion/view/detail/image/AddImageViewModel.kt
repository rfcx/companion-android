package org.rfcx.companion.view.detail.image

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import org.rfcx.companion.view.deployment.Image
import org.rfcx.companion.view.detail.toImage

class AddImageViewModel(
    application: Application,
    private val addImageRepository: AddImageRepository
) : AndroidViewModel(application) {

    fun getImages(deploymentId: Int?): List<Image> {
        if (deploymentId == -1 || deploymentId == null) return listOf()
        return addImageRepository.getImages(deploymentId).map { it.toImage() }
    }

    fun saveImages(images: List<Image>, deploymentId: Int?) {
        if (deploymentId == -1 || deploymentId == null) return
        addImageRepository.saveImages(images, deploymentId)
    }
}
