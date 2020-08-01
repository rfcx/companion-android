package org.rfcx.audiomoth

import io.realm.DynamicRealm
import io.realm.RealmMigration
import org.rfcx.audiomoth.entity.Deployment
import java.util.*

class CompanionRealmMigration : RealmMigration {
    override fun migrate(realm: DynamicRealm, oldVersion: Long, newVersion: Long) {
        if (oldVersion < 2L && newVersion >= 2L) {
            migrateToV2(realm)
        }
        if (oldVersion < 3L && newVersion >= 3L) {
            migrateToV3(realm)
        }
    }

    private fun migrateToV2(realm: DynamicRealm) {
        // Add field deploymentId to Deployment
        val deployment = realm.schema.get(Deployment.TABLE_NAME)
        deployment?.apply {
            addField(Deployment.FIELD_DEPLOYMENT_ID, String::class.java)
        }
    }

    private fun migrateToV3(realm: DynamicRealm) {
        // Add field updatedAt to Deployment
        val deployment = realm.schema.get(Deployment.TABLE_NAME)
        deployment?.apply {
            addField(Deployment.FIELD_UPDATED_AT, Date::class.java)
        }
    }

    override fun hashCode(): Int {
        return 1
    }

    override fun equals(other: Any?): Boolean {
        return other.hashCode() == hashCode()
    }
}
