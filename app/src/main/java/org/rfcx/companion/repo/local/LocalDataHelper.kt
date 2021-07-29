package org.rfcx.companion.repo.local

import io.realm.Realm
import org.rfcx.companion.localdb.*
import org.rfcx.companion.localdb.guardian.GuardianDeploymentDb
import org.rfcx.companion.util.RealmHelper

class LocalDataHelper {
    private val realm by lazy { Realm.getInstance(RealmHelper.migrationConfig()) }
    private val locateDb by lazy { LocateDb(realm) }
    private val projectDb by lazy { ProjectDb(realm) }
    private val trackingDb by lazy { TrackingDb(realm) }
    private val deploymentDb by lazy { EdgeDeploymentDb(realm) }
    private val trackingFileDb by lazy { TrackingFileDb(realm) }
    private val guardianDeploymentDb by lazy { GuardianDeploymentDb(realm) }

    fun getLocateLocalDb() = locateDb
    fun getProjectLocalDb() = projectDb
    fun getTrackingLocalDb() = trackingDb
    fun getDeploymentLocalDb() = deploymentDb
    fun getTrackingFileLocalDb() = trackingFileDb
    fun getGuardianDeploymentLocalDb() = guardianDeploymentDb

}
