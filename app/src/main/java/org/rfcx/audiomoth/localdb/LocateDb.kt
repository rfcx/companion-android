package org.rfcx.audiomoth.localdb

import io.realm.Realm
import io.realm.RealmResults
import io.realm.Sort
import org.rfcx.audiomoth.entity.Locate

class LocateDb(private val realm: Realm) {

    fun getAllResultsAsync(sort: Sort = Sort.DESCENDING): RealmResults<Locate> {
        return realm.where(Locate::class.java)
            .sort(Locate.FIELD_ID, sort)
            .findAllAsync()
    }

    fun getLocations(): List<Locate> {
        return realm.where(Locate::class.java).findAll() ?: arrayListOf()
    }

    fun getLocateById(id: Int): Locate? {
        return realm.where(Locate::class.java).equalTo(Locate.FIELD_ID, id)
            .findFirst()
    }

    fun updateLocate(deploymentId: Int, locate: Locate) {
        realm.executeTransaction {
            if (locate.id == 0) {
                val id = (realm.where(Locate::class.java).max(Locate.FIELD_ID)
                    ?.toInt() ?: 0) + 1
                locate.id = id
            }
            // update last deployment
            locate.lastDeployment = deploymentId
            it.insertOrUpdate(locate)
        }
    }
}