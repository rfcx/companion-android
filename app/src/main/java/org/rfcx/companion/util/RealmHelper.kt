package org.rfcx.companion.util

import io.realm.RealmConfiguration
import org.rfcx.companion.CompanionRealmMigration

class RealmHelper {
    companion object {
        const val schemaVersion = 21L

        fun migrationConfig(): RealmConfiguration {
            return RealmConfiguration.Builder().apply {
                schemaVersion(schemaVersion)
                migration(CompanionRealmMigration())
                allowWritesOnUiThread(true)
            }.build()
        }

        fun fallbackConfig(): RealmConfiguration {
            return RealmConfiguration.Builder().apply {
                schemaVersion(schemaVersion)
                deleteRealmIfMigrationNeeded()
            }.build()
        }
    }
}
