package org.rfcx.companion.repo.local

import io.realm.Realm
import org.rfcx.companion.localdb.ProjectDb
import org.rfcx.companion.localdb.TrackingFileDb
import org.rfcx.companion.util.RealmHelper

class LocalDataHelper {
    private val realm by lazy { Realm.getInstance(RealmHelper.migrationConfig()) }
    private val projectDb by lazy { ProjectDb(realm) }
    private val trackingFileDb by lazy { TrackingFileDb(realm) }

    fun getProjectLocalDb()  = projectDb
    fun getTrackingFileLocalDb()  = trackingFileDb
}
