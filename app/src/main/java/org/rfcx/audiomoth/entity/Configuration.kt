package org.rfcx.audiomoth.entity

import io.realm.RealmList
import io.realm.RealmModel
import io.realm.annotations.RealmClass

@RealmClass
open class Configuration(
    var gain: Int = 3,
    var sampleRate: Int = 8,
    var recordingDuration: Int = 5,
    var sleepDuration: Int = 10,
    var recordingPeriodList: RealmList<String> = RealmList(),
    var durationSelected: String = "RECOMMENDED"
) : RealmModel