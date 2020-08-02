package org.rfcx.audiomoth

import io.realm.DynamicRealm
import io.realm.RealmMigration
import org.rfcx.audiomoth.entity.EdgeDeployment
import org.rfcx.audiomoth.entity.Locate
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
        val deployment = realm.schema.get("Deployment")
        deployment?.apply {
            addField("deploymentId", String::class.java)
        }
    }

    private fun migrateToV3(realm: DynamicRealm) {
        // Rename table Configuration to EdgeConfiguration
        val edgeConfiguration = realm.schema.rename("Configuration", "EdgeConfiguration")

        // Rename table Deployment to EdgeDeployment
        val edgeDeployment = realm.schema.rename("Deployment", "EdgeDeployment")

        // Add field updatedAt and deletedAt to EdgeDeployment
        edgeDeployment?.apply {
            // Change Configuration class to EdgeConfiguration class
            addRealmObjectField("configuration_tmp", edgeConfiguration)
            transform { obj ->
                val configObj = obj.getObject("configuration")
                obj.setObject("configuration_tmp", configObj)
            }
            removeField("configuration")
            renameField("configuration_tmp", "configuration")


            addField(EdgeDeployment.FIELD_UPDATED_AT, Date::class.java)
            addField(EdgeDeployment.FIELD_DELETED_AT, Date::class.java)
        }

        val locate = realm.schema.get(Locate.TABLE_NAME)
        locate?.apply {
            addField(Locate.FIELD_DELETED_AT, Date::class.java)
        }
    }

    override fun hashCode(): Int {
        return 1
    }

    override fun equals(other: Any?): Boolean {
        return other.hashCode() == hashCode()
    }
}
