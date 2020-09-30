package org.rfcx.audiomoth

import io.realm.DynamicRealm
import io.realm.RealmMigration
import java.util.*
import org.rfcx.audiomoth.entity.EdgeDeployment
import org.rfcx.audiomoth.entity.Locate

class CompanionRealmMigration : RealmMigration {
    override fun migrate(realm: DynamicRealm, oldVersion: Long, newVersion: Long) {
        if (oldVersion < 2L && newVersion >= 2L) {
            migrateToV2(realm)
        }
        if (oldVersion < 3L && newVersion >= 3L) {
            migrateToV3(realm)
        }
        if (oldVersion < 4L && newVersion >= 4L) {
            migrateToV4(realm)
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
        val edgeDeployment = realm.schema.rename("Deployment", EdgeDeployment.TABLE_NAME)

        // Add field updatedAt and deletedAt to EdgeDeployment
        edgeDeployment?.apply {
            // Change Configuration class to EdgeConfiguration class
            addRealmObjectField("configuration_tmp", edgeConfiguration)
            transform { obj ->
                val configObj = obj.getObject("configuration_tmp")
                obj.setObject("configuration_tmp", configObj)
            }
            removeField("configuration_tmp")
            renameField(
                "configuration_tmp",
                "configuration_tmp"
            )

            addField(EdgeDeployment.FIELD_UPDATED_AT, Date::class.java)
            addField(EdgeDeployment.FIELD_DELETED_AT, Date::class.java)
        }

        val locate = realm.schema.get(Locate.TABLE_NAME)
        locate?.apply {
            addField(Locate.FIELD_DELETED_AT, Date::class.java)
        }
    }

    private fun migrateToV4(realm: DynamicRealm) {
        // Remove tables that were not used in AudioMoth version
        realm.schema.remove("Profile")
        realm.schema.remove("EdgeConfiguration")

        // Remove fields that were not used in AudioMoth version
        val edgeDeployment = realm.schema.get(EdgeDeployment.TABLE_NAME)
        edgeDeployment?.apply {
            removeField("configuration")
            removeField("batteryLevel")
            removeField("batteryDepletedAt")
        }
    }

    override fun hashCode(): Int {
        return 1
    }

    override fun equals(other: Any?): Boolean {
        return other.hashCode() == hashCode()
    }
}
