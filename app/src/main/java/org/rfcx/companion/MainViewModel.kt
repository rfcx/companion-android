package org.rfcx.companion

import android.app.Application
import androidx.lifecycle.AndroidViewModel

class MainViewModel(
    application: Application,
    private val mainRepository: MainRepository
) : AndroidViewModel(application) {
}
