package org.rfcx.companion

import android.app.Application
import io.realm.Realm
import io.realm.exceptions.RealmMigrationNeededException
import org.rfcx.companion.connection.socket.SocketManager
import org.rfcx.companion.service.DeploymentCleanupWorker
import org.rfcx.companion.util.RealmHelper

class CompanionApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        Realm.init(this)
        setupRealm()
        DeploymentCleanupWorker.enqueuePeriodically(this)
    }

    private fun setupRealm() {
        var realmNeedsMigration = false
        try {
            val realm = Realm.getInstance(RealmHelper.migrationConfig())
            realm.close()
            Realm.setDefaultConfiguration(RealmHelper.migrationConfig())
        } catch (e: RealmMigrationNeededException) {
            realmNeedsMigration = true
        }

        // Falback for release (delete realm on error)
        if (realmNeedsMigration && !BuildConfig.DEBUG) {
            try {
                val realm = Realm.getInstance(RealmHelper.fallbackConfig())
                realm.close()
            } catch (e: RealmMigrationNeededException) {
            }
        }
    }

    override fun onTerminate() {
        super.onTerminate()
        SocketManager.stopConnection()
    }
}
