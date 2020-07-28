package org.rfcx.audiomoth.entity

import io.realm.RealmList
import io.realm.RealmModel
import io.realm.annotations.RealmClass
import org.rfcx.audiomoth.util.EdgeConfigure

@RealmClass
open class Configuration(
    var gain: Int = EdgeConfigure.GAIN_DEFAULT,
    var sampleRate: Int = EdgeConfigure.SAMPLE_RATE_DEFAULT,
    var recordingDuration: Int = EdgeConfigure.RECORDING_DURATION_DEFAULT,
    var sleepDuration: Int = EdgeConfigure.SLEEP_DURATION_DEFAULT,
    var recordingPeriodList: RealmList<String> = RealmList(),
    var durationSelected: String = EdgeConfigure.DURATION_SELECTED_DEFAULT
) : RealmModel
