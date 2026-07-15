package org.rfcx.companion

import android.app.Application
import com.google.firebase.FirebaseApp
import com.posthog.android.PostHogAndroid
import com.posthog.android.PostHogAndroidConfig
import io.realm.Realm
import io.realm.exceptions.RealmMigrationNeededException
import org.rfcx.companion.service.DeploymentCleanupWorker
import org.rfcx.companion.util.*

class CompanionApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        FirebaseApp.initializeApp(this)
        setupPostHog()
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
        }
    }

    // Self-hosted PostHog product analytics (replaces Firebase Analytics).
    // Conservative config, consistent with the other rfcx clients: no screen-view
    // autocapture (screens are sent manually via Analytics.trackScreen) and no UI
    // autocapture / session replay. App lifecycle events are kept (low-noise).
    private fun setupPostHog() {
        val config = PostHogAndroidConfig(
            apiKey = BuildConfig.POSTHOG_API_KEY,
            host = BuildConfig.POSTHOG_HOST
        ).apply {
            captureScreenViews = false
            captureDeepLinks = false
            sessionReplay = false
        }
        PostHogAndroid.setup(this, config)
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
}
