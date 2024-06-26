package org.rfcx.companion.util

import io.realm.RealmConfiguration
import org.rfcx.companion.CompanionRealmMigration

class RealmHelper {
    companion object {
        const val schemaVersion = 22L

        fun migrationConfig(): RealmConfiguration {
            return RealmConfiguration.Builder().apply {
                allowWritesOnUiThread(true)
                allowQueriesOnUiThread(true)
                schemaVersion(schemaVersion)
                migration(CompanionRealmMigration())
            }.build()
        }

        fun fallbackConfig(): RealmConfiguration {
            return RealmConfiguration.Builder().apply {
                allowWritesOnUiThread(true)
                allowQueriesOnUiThread(true)
                schemaVersion(schemaVersion)
                deleteRealmIfMigrationNeeded()
            }.build()
        }
    }
}
