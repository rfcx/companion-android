package org.rfcx.audiomoth.entity

import io.realm.RealmList
import io.realm.RealmModel
import io.realm.annotations.RealmClass
import org.rfcx.audiomoth.entity.EdgeConfigure.Companion.DURATION_SELECTED_DEFAULT
import org.rfcx.audiomoth.entity.EdgeConfigure.Companion.GAIN_DEFAULT
import org.rfcx.audiomoth.entity.EdgeConfigure.Companion.RECORDING_DURATION_DEFAULT
import org.rfcx.audiomoth.entity.EdgeConfigure.Companion.SAMPLE_RATE_DEFAULT
import org.rfcx.audiomoth.entity.EdgeConfigure.Companion.SLEEP_DURATION_DEFAULT

@RealmClass
open class Configuration(
    var gain: Int = GAIN_DEFAULT,
    var sampleRate: Int = SAMPLE_RATE_DEFAULT,
    var recordingDuration: Int = RECORDING_DURATION_DEFAULT,
    var sleepDuration: Int = SLEEP_DURATION_DEFAULT,
    var recordingPeriodList: RealmList<String> = RealmList(),
    var durationSelected: String = DURATION_SELECTED_DEFAULT
) : RealmModel
