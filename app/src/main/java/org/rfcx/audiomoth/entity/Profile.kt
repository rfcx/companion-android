package org.rfcx.audiomoth.entity

import com.google.gson.annotations.Expose
import io.realm.RealmList
import io.realm.RealmModel
import io.realm.annotations.PrimaryKey
import io.realm.annotations.RealmClass
import java.util.*
import org.rfcx.audiomoth.util.EdgeConfigure

@RealmClass
open class Profile(
    @PrimaryKey
    var id: Int = 0,
    var serverId: String? = null,
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
    constructor(
        gain: Int,
        name: String,
        sampleRate: Int,
        recordingDuration: Int,
        sleepDuration: Int,
        recordingPeriodList: ArrayList<String>,
        durationSelected: String
    ) : this() {
        this.gain = gain
        this.name = name
        this.sampleRate = sampleRate
        this.recordingDuration = recordingDuration
        this.sleepDuration = sleepDuration
        this.recordingPeriodList = recordingPeriodList.mapTo(RealmList(), { it })
        this.durationSelected = durationSelected
    }

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
        const val FIELD_ID = "id"
        const val FIELD_SYNC_STATE = "syncState"
        const val FIELD_NAME = "name"

        fun default() = Profile(
            gain = EdgeConfigure.GAIN_DEFAULT,
            name = "",
            sampleRate = EdgeConfigure.SAMPLE_RATE_DEFAULT,
            recordingDuration = EdgeConfigure.RECORDING_DURATION_DEFAULT,
            sleepDuration = EdgeConfigure.SLEEP_DURATION_DEFAULT,
            durationSelected = EdgeConfigure.DURATION_SELECTED_DEFAULT
        )
    }
}
