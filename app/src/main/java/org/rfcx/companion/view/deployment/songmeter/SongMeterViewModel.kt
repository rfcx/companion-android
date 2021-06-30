package org.rfcx.companion.view.deployment.songmeter

import android.annotation.SuppressLint
import android.app.Application
import androidx.lifecycle.AndroidViewModel

class SongMeterViewModel(
    application: Application,
    private val songMeterRepository: SongMeterRepository
) : AndroidViewModel(application) {
    @SuppressLint("StaticFieldLeak")
    private val context = getApplication<Application>().applicationContext
}
