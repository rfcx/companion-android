package org.rfcx.companion.repo.local

import io.realm.Realm
import org.rfcx.companion.localdb.DeploymentDb
import org.rfcx.companion.localdb.ProjectDb
import org.rfcx.companion.localdb.guardian.GuardianDeploymentDb
import org.rfcx.companion.util.RealmHelper

class LocalDataHelper {
    private val realm by lazy { Realm.getInstance(RealmHelper.migrationConfig()) }
    private val projectDb by lazy { ProjectDb(realm) }
    private val deploymentDb by lazy { DeploymentDb(realm) }
    private val guardianDeploymentDb by lazy { GuardianDeploymentDb(realm) }

    fun getProjectLocalDb()  = projectDb
    fun getDeploymentLocalDb()  = deploymentDb
    fun getGuardianDeploymentLocalDb()  = guardianDeploymentDb
}
