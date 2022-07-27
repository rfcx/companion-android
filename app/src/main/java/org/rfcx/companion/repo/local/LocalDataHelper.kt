package org.rfcx.companion.repo.local

import io.realm.Realm
import org.rfcx.companion.localdb.*
import org.rfcx.companion.localdb.DeploymentDb
import org.rfcx.companion.util.RealmHelper

class LocalDataHelper {
    private val realm by lazy { Realm.getInstance(RealmHelper.migrationConfig()) }
    private val streamDb by lazy { StreamDb(realm) }
    private val projectDb by lazy { ProjectDb(realm) }
    private val trackingDb by lazy { TrackingDb(realm) }
    private val trackingFileDb by lazy { TrackingFileDb(realm) }
    private val deploymentImageDb by lazy { DeploymentImageDb(realm) }
    private val deploymentDb by lazy { DeploymentDb(realm) }
    private val classifierDb by lazy { ClassifierDb(realm) }

    fun getStreamLocalDb() = streamDb
    fun getProjectLocalDb() = projectDb
    fun getTrackingLocalDb() = trackingDb
    fun getTrackingFileLocalDb() = trackingFileDb
    fun getDeploymentImageLocalDb() = deploymentImageDb
    fun getDeploymentLocalDb() = deploymentDb
    fun getClassifierLocalDb() = classifierDb
}
