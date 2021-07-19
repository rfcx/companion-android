package org.rfcx.companion.repo.local

import io.realm.Realm
import org.rfcx.companion.localdb.EdgeDeploymentDb
import org.rfcx.companion.localdb.LocateDb
import org.rfcx.companion.localdb.ProjectDb
import org.rfcx.companion.localdb.TrackingFileDb
import org.rfcx.companion.localdb.guardian.GuardianDeploymentDb
import org.rfcx.companion.util.RealmHelper

class LocalDataHelper {
    private val realm by lazy { Realm.getInstance(RealmHelper.migrationConfig()) }
    private val projectDb by lazy { ProjectDb(realm) }
    private val locateDb by lazy { LocateDb(realm) }
    private val deploymentDb by lazy { EdgeDeploymentDb(realm) }
    private val trackingFileDb by lazy { TrackingFileDb(realm) }
    private val guardianDeploymentDb by lazy { GuardianDeploymentDb(realm) }


    fun getProjectLocalDb()  = projectDb
    fun getLocateLocalDb()  = locateDb
    fun getDeploymentLocalDb()  = deploymentDb
    fun getTrackingFileLocalDb()  = trackingFileDb
    fun getGuardianDeploymentLocalDb()  = guardianDeploymentDb

}
