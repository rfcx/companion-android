package org.rfcx.audiomoth.util

import io.realm.RealmConfiguration
import org.rfcx.audiomoth.CompanionRealmMigration

class RealmHelper {
    companion object {
        private const val schemaVersion = 3L

        fun migrationConfig(): RealmConfiguration {
            return RealmConfiguration.Builder().apply {
                schemaVersion(schemaVersion)
                migration(CompanionRealmMigration())
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
