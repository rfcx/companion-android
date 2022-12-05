package org.rfcx.companion

import android.app.Application
import com.google.firebase.FirebaseApp
import io.realm.Realm
import io.realm.exceptions.RealmMigrationNeededException
import org.rfcx.companion.service.DeploymentCleanupWorker
import org.rfcx.companion.util.*

class CompanionApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        FirebaseApp.initializeApp(this)
        Realm.init(this)
        setupRealm()
        DeploymentCleanupWorker.enqueuePeriodically(this)

        val preferences = Preferences.getInstance(this)
        val state = preferences.getBoolean(Preferences.ENABLE_LOCATION_TRACKING, false)
        if (state) {
            LocationTrackingManager.set(this, true)
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
        } catch (e: RealmMigrationNeededException) {
            realmNeedsMigration = true
        }

        // Fallback for release (delete realm on error)
        if (realmNeedsMigration) {
            try {
                val realm = Realm.getInstance(RealmHelper.fallbackConfig())
                realm.close()
            } catch (e: RealmMigrationNeededException) {
                logout()
            }
        }
    }

    override fun onTerminate() {
        super.onTerminate()
        SocketUtils.stopAllConnections()
    }
}
