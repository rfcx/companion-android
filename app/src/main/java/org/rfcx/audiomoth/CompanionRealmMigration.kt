package org.rfcx.audiomoth

import io.realm.DynamicRealm
import io.realm.FieldAttribute
import io.realm.RealmMigration
import org.rfcx.audiomoth.entity.*
import java.util.*

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
        val edgeConfiguration = realm.schema.rename("Configuration", EdgeConfiguration.TABLE_NAME)

        // Rename table Deployment to EdgeDeployment
        val edgeDeployment = realm.schema.rename("Deployment", EdgeDeployment.TABLE_NAME)

        // Add field updatedAt and deletedAt to EdgeDeployment
        edgeDeployment?.apply {
            // Change Configuration class to EdgeConfiguration class
            addRealmObjectField("${EdgeDeployment.FIELD_CONFIGURATION}_tmp", edgeConfiguration)
            transform { obj ->
                val configObj = obj.getObject(EdgeDeployment.FIELD_CONFIGURATION)
                obj.setObject("${EdgeDeployment.FIELD_CONFIGURATION}_tmp", configObj)
            }
            removeField(EdgeDeployment.FIELD_CONFIGURATION)
            renameField(
                "${EdgeDeployment.FIELD_CONFIGURATION}_tmp",
                EdgeDeployment.FIELD_CONFIGURATION
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
        // Add LocationGroups class
        val locationGroups = realm.schema.create(LocationGroups.TABLE_NAME)
        locationGroups.apply {
            addField(LocationGroups.LOCATION_GROUPS_ID, Int::class.java, FieldAttribute.PRIMARY_KEY)
            addField(
                LocationGroups.LOCATION_GROUPS_NAME,
                String::class.java
            ).setNullable(LocationGroups.LOCATION_GROUPS_NAME, false)

            addField(LocationGroups.LOCATION_GROUPS_COLOR, String::class.java)
                .setNullable(LocationGroups.LOCATION_GROUPS_COLOR, false)

            addField(LocationGroups.LOCATION_GROUPS_SYNC_STATE, Int::class.java)
            addField(LocationGroups.LOCATION_GROUPS_SERVER_ID, String::class.java)
        }

        // Add LocationGroup class
        val locationGroup = realm.schema.create(LocationGroup.TABLE_NAME)
        locationGroup.apply {
            addField(LocationGroup.FIELD_GROUP, String::class.java)
                .setNullable(LocationGroup.FIELD_GROUP, false)

            addField(LocationGroup.FIELD_COLOR, String::class.java)
                .setNullable(LocationGroup.FIELD_COLOR, false)

            addField(LocationGroup.FIELD_SERVER_ID, String::class.java)
                .setNullable(LocationGroup.FIELD_SERVER_ID, false)
        }

        val locate = realm.schema.get(Locate.TABLE_NAME)
        locate?.apply {
            addRealmObjectField(Locate.FIELD_LOCATION_GROUP, locationGroup)
        }

        val deploymentLocation = realm.schema.get(DeploymentLocation.TABLE_NAME)
        deploymentLocation?.apply {
            addRealmObjectField(DeploymentLocation.FIELD_LOCATION_GROUP, locationGroup)
        }
    }

    override fun hashCode(): Int {
        return 1
    }

    override fun equals(other: Any?): Boolean {
        return other.hashCode() == hashCode()
    }
}
