package org.rfcx.audiomoth.entity.guardian

import io.realm.RealmModel
import io.realm.annotations.RealmClass

@RealmClass
open class GuardianConfiguration(
    val sampleRate: Int = 14,
    val bitrate: Int = 14,
    val fileFormat: String = "OPUS",
    val duration: Int = 90
) : RealmModel
