package org.rfcx.companion.entity

import io.realm.RealmList
import io.realm.RealmObject
import io.realm.annotations.PrimaryKey
import io.realm.annotations.RealmClass
import java.util.*

@RealmClass
open class Tracking(
    @PrimaryKey
    var id: Int = 0,
    var startAt: Date = Date(),
    var stopAt: Date? = null,
    var points: RealmList<Coordinate> = RealmList()
) : RealmObject() {
    companion object {
        const val TABLE_NAME = "Tracking"
        const val TRACKING_ID = "id"
        const val TRACKING_START_AT = "startAt"
        const val TRACKING_STOP_AT = "stopAt"
        const val TRACKING_POINTS = "points"
    }
}

fun RealmList<Coordinate>.toListDoubleArray(): List<DoubleArray> {
    return this.map {
        listOf(it.longitude, it.latitude).toDoubleArray()
    }.toList()
}
