package org.rfcx.audiomoth.entity.guardian

import com.google.gson.annotations.Expose
import io.realm.RealmModel
import io.realm.annotations.PrimaryKey
import io.realm.annotations.RealmClass
import java.util.*

@RealmClass
open class GuardianProfile(
    @PrimaryKey
    var id: Int = 0,
    var serverId: String? = null,
    var name: String = "",
    var sampleRate: Int = 0,
    var bitrate: Int = 0,
    var fileFormat: String = "",
    var duration: Int = 0,
    var createdAt: Date = Date(),
    @Expose(serialize = false)
    var syncState: Int = 0
) : RealmModel {
    constructor(
        name: String,
        sampleRate: Int,
        bitrate: Int,
        fileFormat: String,
        duration: Int
    ) : this() {
        this.name = name
        this.sampleRate = sampleRate
        this.bitrate = bitrate
        this.fileFormat = fileFormat
        this.duration = duration
    }

    fun isNew() = (this.id == 0)

    fun asConfiguration(): GuardianConfiguration {
        return GuardianConfiguration(
            sampleRate = sampleRate,
            bitrate = bitrate,
            fileFormat = fileFormat,
            duration = duration
        )
    }

    companion object {
        const val FIELD_ID = "id"
        const val FIELD_SYNC_STATE = "syncState"

        fun default() = GuardianProfile(
            name = "",
            sampleRate = 24000,
            bitrate = 28672,
            fileFormat = "opus",
            duration = 90
        )
    }
}
