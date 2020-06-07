package org.rfcx.audiomoth.localdb

import io.realm.Realm
import org.rfcx.audiomoth.entity.DeploymentLocation
import org.rfcx.audiomoth.entity.Locate

class LocateDb(private val realm: Realm) {

    fun getLocations(): List<Locate> {
        return realm.where(Locate::class.java).findAll() ?: arrayListOf()
    }

    fun getLocateById(id: Int): Locate? {
        return realm.where(Locate::class.java).equalTo(Locate.FIELD_ID, id)
            .findFirst()
    }

    fun insertLocate(locate: Locate): Int {
        var id = locate.id
        realm.executeTransaction {
            if (locate.id == 0) {
                id = (realm.where(Locate::class.java).max(Locate.FIELD_ID)
                    ?.toInt() ?: 0) + 1
                locate.id = id
            }
            it.insertOrUpdate(locate)
        }
        return id
    }
}