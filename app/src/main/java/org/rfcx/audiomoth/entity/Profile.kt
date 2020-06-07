package org.rfcx.audiomoth.entity

import com.google.gson.annotations.Expose
import io.realm.RealmList
import io.realm.RealmModel
import io.realm.annotations.PrimaryKey
import io.realm.annotations.RealmClass
import org.rfcx.audiomoth.view.configure.ConfigureFragment
import java.util.*

@RealmClass
open class Profile(
    @PrimaryKey
    var id: Int = 0,
    var gain: Int = 0,
    var name: String = "",
    var sampleRate: Int = 0,
    var recordingDuration: Int = 0,
    var sleepDuration: Int = 0,
    var recordingPeriodList: RealmList<String> = RealmList(),
    var durationSelected: String = "",
    var createdAt: Date = Date(),
    @Expose(serialize = false)
    var syncState: Int = 0
) : RealmModel {
    fun asConfiguration(): Configuration {
        return Configuration(
            gain = gain,
            sampleRate = sampleRate,
            recordingDuration = recordingDuration,
            sleepDuration = sleepDuration,
            recordingPeriodList = recordingPeriodList,
            durationSelected = durationSelected
        )
    }

    companion object {
        fun default() = Profile(
            gain = 3,
            name = "",
            sampleRate = 8,
            recordingDuration = 5,
            sleepDuration = 10,
            durationSelected = ConfigureFragment.RECOMMENDED
        )
    }
}