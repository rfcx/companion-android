package org.rfcx.companion

import io.realm.DynamicRealm
import io.realm.FieldAttribute
import io.realm.RealmMigration
import org.rfcx.companion.entity.*
import java.util.*
import org.rfcx.companion.entity.EdgeDeployment
import org.rfcx.companion.entity.Locate
import org.rfcx.companion.entity.guardian.GuardianDeployment

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
        if (oldVersion < 5L && newVersion >= 5L) {
            migrateToV5(realm)
        }
        if (oldVersion < 6L && newVersion >= 6L) {
            migrateToV6(realm)
        }
        if (oldVersion < 7L && newVersion >= 7L) {
            migrateToV7(realm)
        }
        if (oldVersion < 8L && newVersion >= 8L) {
            migrateToV8(realm)
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
        // Add LocationGroup class
        val locationGroup = realm.schema.create(LocationGroup.TABLE_NAME)
        locationGroup.apply {
            addField(LocationGroup.FIELD_GROUP, String::class.java)
            addField(LocationGroup.FIELD_COLOR, String::class.java)
            addField(LocationGroup.FIELD_SERVER_ID, String::class.java)
        }

        // Add LocationGroups class
        val locationGroups = realm.schema.create(LocationGroups.TABLE_NAME)
        locationGroups.apply {
            addField(
                LocationGroups.LOCATION_GROUPS_ID,
                Int::class.java,
                FieldAttribute.PRIMARY_KEY
            )
            addField(
                LocationGroups.LOCATION_GROUPS_NAME,
                String::class.java
            ).setNullable(LocationGroups.LOCATION_GROUPS_NAME, false)

            addField(LocationGroups.LOCATION_GROUPS_COLOR, String::class.java)
                .setNullable(LocationGroups.LOCATION_GROUPS_COLOR, false)

            addField(LocationGroups.LOCATION_GROUPS_SYNC_STATE, Int::class.java)
            addField(LocationGroups.LOCATION_GROUPS_SERVER_ID, String::class.java)
            addField(LocationGroups.LOCATION_GROUPS_DELETE_AT, Date::class.java)
        }

        val locate = realm.schema.get(Locate.TABLE_NAME)
        locate?.apply {
            addRealmObjectField(Locate.FIELD_LOCATION_GROUP, locationGroup)
        }

        val deploymentLocation = realm.schema.get(DeploymentLocation.TABLE_NAME)
        deploymentLocation?.apply {
            addRealmObjectField(DeploymentLocation.FIELD_LOCATION_GROUP, locationGroup)
        }

        // Delete field first to avoid ref error
        // Remove fields that were not used in AudioMoth version
        val edgeDeployment = realm.schema.get(EdgeDeployment.TABLE_NAME)
        edgeDeployment?.apply {
            val hasConfigField = this.hasField("configuration")
            val hasBatteryLevelField = this.hasField("batteryLevel")
            val hasBatteryDepletedField = this.hasField("batteryDepletedAt")
            if (hasConfigField) {
                removeField("configuration")
            }
            if (hasBatteryLevelField) {
                removeField("batteryLevel")
            }
            if (hasBatteryDepletedField) {
                removeField("batteryDepletedAt")
            }
        }

        // Remove tables that were not used in AudioMoth version
        val edgeProfile = realm.schema.get("Profile")
        val edgeConfig = realm.schema.get("EdgeConfiguration")
        edgeProfile?.let {
            realm.schema.remove("Profile")
        }
        edgeConfig?.let {
            realm.schema.remove("EdgeConfiguration")
        }
    }

    private fun migrateToV5(realm: DynamicRealm) {
        val edgeDeployment = realm.schema.get(EdgeDeployment.TABLE_NAME)
        edgeDeployment?.apply {
            addRealmListField(EdgeDeployment.FIELD_PASSED_CHECKS,  Int::class.java)
                .setNullable(EdgeDeployment.FIELD_PASSED_CHECKS, true)
        }
    }

    private fun migrateToV6(realm: DynamicRealm) {
        val guardianDeployment = realm.schema.get(GuardianDeployment.TABLE_NAME)
        guardianDeployment?.apply {
            addField(GuardianDeployment.FIELD_UPDATED_AT, Date::class.java)
        }
    }
    
    private fun migrateToV7(realm: DynamicRealm) {
        val locationGroups = realm.schema.get(LocationGroups.TABLE_NAME)
        locationGroups?.apply {
            setNullable(LocationGroups.LOCATION_GROUPS_NAME, true)
            setNullable(LocationGroups.LOCATION_GROUPS_COLOR, true)
        }
    }

    private fun migrateToV8(realm: DynamicRealm) {
        val locate = realm.schema.get(Locate.TABLE_NAME)
        locate?.apply {
            addField(Locate.FIELD_ALTITUDE, Double::class.java)
        }

        val deploymentLocation = realm.schema.get(DeploymentLocation.TABLE_NAME)
        deploymentLocation?.apply {
            addField(DeploymentLocation.FIELD_ALTITUDE, Double::class.java)
        }

        val deploymentImage = realm.schema.get(DeploymentImage.TABLE_NAME)
        deploymentImage?.apply {
            addField(DeploymentImage.FIELD_DEVICE, String::class.java)
                .setNullable(DeploymentImage.FIELD_DEVICE, false)
        }
    }

    override fun hashCode(): Int {
        return 1
    }

    override fun equals(other: Any?): Boolean {
        return other.hashCode() == hashCode()
    }
}
