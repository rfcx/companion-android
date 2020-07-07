package org.rfcx.audiomoth.entity

import io.realm.RealmList
import io.realm.RealmModel
import io.realm.annotations.RealmClass
import org.rfcx.audiomoth.view.deployment.configure.ConfigureFragment

@RealmClass
open class Configuration(
    var gain: Int = GAIN_DEFAULT,
    var sampleRate: Int = SAMPLE_RATE_DEFAULT,
    var recordingDuration: Int = RECORDING_DURATION_DEFAULT,
    var sleepDuration: Int = SLEEP_DURATION_DEFAULT,
    var recordingPeriodList: RealmList<String> = RealmList(),
    var durationSelected: String = DURATION_SELECTED_DEFAULT
) : RealmModel {
    companion object {
        const val GAIN_DEFAULT = 2
        const val SAMPLE_RATE_DEFAULT = 48
        const val RECORDING_DURATION_DEFAULT = 1
        const val SLEEP_DURATION_DEFAULT = 5
        const val DURATION_SELECTED_DEFAULT = ConfigureFragment.RECOMMENDED
    }
}