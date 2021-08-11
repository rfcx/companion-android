package org.rfcx.companion.view.deployment

import android.app.Application
import androidx.lifecycle.AndroidViewModel

class AudioMothDeploymentViewModel(
    application: Application,
    private val audioMothDeploymentRepository: AudioMothDeploymentRepository
) : AndroidViewModel(application) {
}
