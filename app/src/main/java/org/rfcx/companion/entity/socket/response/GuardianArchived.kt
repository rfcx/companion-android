package org.rfcx.companion.entity.socket.response

import android.os.Parcel
import android.os.Parcelable

data class GuardianArchivedCoverage(
    val maximumFileCount: Int,
    val listOfFile: List<Long>
)

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
    )

    fun toListOfTimestamp(): GuardianArchivedCoverage {
        // Calculate all timestamp
        val timestamps = arrayListOf<Long>()
        val durationSecond = duration * 1000
        for (i in 0 until count) {
            timestamps.add(archivedStart + (durationSecond * i) + (durationSecond * skipping))
        }

        // Calculate maximum file count per hour of this archive
        val maximum = (1000 * 60 * 60) / (durationSecond + (durationSecond * skipping))
        return GuardianArchivedCoverage(maximum, timestamps)
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
