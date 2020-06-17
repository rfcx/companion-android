package org.rfcx.audiomoth.entity.guardian

import com.google.gson.annotations.Expose
import io.realm.RealmModel
import io.realm.annotations.PrimaryKey
import java.util.*

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

        fun default() = GuardianProfile(
            name = "",
            sampleRate = 14,
            bitrate = 14,
            fileFormat = "OPUS",
            duration = 90
        )
    }
}
