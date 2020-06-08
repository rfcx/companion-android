package org.rfcx.audiomoth.util

import io.realm.RealmConfiguration

class RealmHelper {
    companion object {
        private const val schemaVersion = 1L

        fun migrationConfig(): RealmConfiguration {
            return RealmConfiguration.Builder().apply {
                schemaVersion(schemaVersion)
                // create migration database here
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