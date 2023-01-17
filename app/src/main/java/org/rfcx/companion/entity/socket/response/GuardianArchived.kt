package org.rfcx.companion.entity.socket.response

import android.os.Parcel
import android.os.Parcelable

data class GuardianArchived(
    val archivedStart: Long,
    val archivedEnd: Long,
    val count: Int,
    val duration: Int,
    val skipping: Int
) : Parcelable {
    constructor(parcel: Parcel) : this(
        parcel.readLong(),
        parcel.readLong(),
        parcel.readInt(),
        parcel.readInt(),
        parcel.readInt()
    ) {
    }

    fun toListOfTimestamp(): List<Long> {
        val timestamps = arrayListOf<Long>()
        for (i in 0 until count) {
            timestamps.add(archivedStart + (duration * 1000 * i) + (duration * 1000 * skipping))
        }
        return timestamps
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeLong(archivedStart)
        parcel.writeLong(archivedEnd)
        parcel.writeInt(count)
        parcel.writeInt(duration)
        parcel.writeInt(skipping)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<GuardianArchived> {
        override fun createFromParcel(parcel: Parcel): GuardianArchived {
            return GuardianArchived(parcel)
        }

        override fun newArray(size: Int): Array<GuardianArchived?> {
            return arrayOfNulls(size)
        }
    }
}
