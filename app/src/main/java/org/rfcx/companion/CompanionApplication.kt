package org.rfcx.companion

import android.app.Application
import io.realm.Realm
import io.realm.exceptions.RealmMigrationNeededException
import org.rfcx.companion.service.DeploymentCleanupWorker
import org.rfcx.companion.util.LocationTracking
import org.rfcx.companion.util.Preferences
import org.rfcx.companion.util.RealmHelper
import org.rfcx.companion.util.SocketUtils

class CompanionApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        Realm.init(this)
        setupRealm()
        DeploymentCleanupWorker.enqueuePeriodically(this)

        val preferences = Preferences.getInstance(this)
        val state = preferences.getBoolean(Preferences.ENABLE_LOCATION_TRACKING, false)
        if (state) {
            LocationTracking.set(this, true)
        }

        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            defaultHandler?.uncaughtException(thread, throwable)

            SocketUtils.stopAllConnections()
        }
    }

    private fun setupRealm() {
        var realmNeedsMigration = false
        try {
            val realm = Realm.getInstance(RealmHelper.migrationConfig())
            realm.close()
            Realm.setDefaultConfiguration(RealmHelper.migrationConfig())
            if (RealmHelper.schemaVersion == 18L) {
                realmNeedsMigration = true
            }
        } catch (e: RealmMigrationNeededException) {
            realmNeedsMigration = true
        }

        // Falback for release (delete realm on error)
        if (realmNeedsMigration && BuildConfig.DEBUG) {
            try {
                val realm = Realm.getInstance(RealmHelper.fallbackConfig())
                realm.close()
            } catch (e: RealmMigrationNeededException) {
            }
        }
    }

    override fun onTerminate() {
        super.onTerminate()
        SocketUtils.stopAllConnections()
    }
}
