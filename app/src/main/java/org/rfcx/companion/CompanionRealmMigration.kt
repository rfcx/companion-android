package org.rfcx.companion

import io.realm.DynamicRealm
import io.realm.FieldAttribute
import io.realm.RealmMigration
import org.rfcx.companion.entity.*
import org.rfcx.companion.entity.guardian.Deployment
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

        if (oldVersion < 9L && newVersion >= 9L) {
            migrateToV9(realm)
        }

        if (oldVersion < 10L && newVersion >= 10L) {
            migrateToV10(realm)
        }

        if (oldVersion < 11L && newVersion >= 11L) {
            migrateToV11(realm)
        }

        if (oldVersion < 12L && newVersion >= 12L) {
            migrateToV12(realm)
        }

        if (oldVersion < 13L && newVersion >= 13L) {
            migrateToV13(realm)
        }

        if (oldVersion < 14L && newVersion >= 14L) {
            migrateToV14(realm)
        }

        if (oldVersion < 15L && newVersion >= 15L) {
            migrateToV15(realm)
        }

        if (oldVersion < 16L && newVersion >= 16L) {
            migrateToV16(realm)
        }

        if (oldVersion < 17L && newVersion >= 17L) {
            migrateToV17(realm)
        }

        if (oldVersion < 18L && newVersion >= 18L) {
            migrateToV18(realm)
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

        // Rename table Deployment to EdgeDeploymentEdgeDeployment
        val edgeDeployment = realm.schema.rename("Deployment", "EdgeDeployment")

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

            addField("updatedAt", Date::class.java)
            addField("deletedAt", Date::class.java)
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
            addField("group", String::class.java)
            addField(LocationGroup.FIELD_COLOR, String::class.java)
            addField("serverId", String::class.java)
        }

        // Add LocationGroups class
        val locationGroups = realm.schema.create(Project.TABLE_NAME)
        locationGroups.apply {
            addField(
                Project.PROJECT_ID,
                Int::class.java,
                FieldAttribute.PRIMARY_KEY
            )
            addField(
                Project.PROJECT_NAME,
                String::class.java
            ).setNullable(Project.PROJECT_NAME, false)

            addField(Project.PROJECT_COLOR, String::class.java)
                .setNullable(Project.PROJECT_COLOR, false)

            addField(Project.PROJECT_SYNC_STATE, Int::class.java)
            addField(Project.PROJECT_SERVER_ID, String::class.java)
            addField(Project.PROJECT_DELETED_AT, Date::class.java)
        }

        val locate = realm.schema.get(Locate.TABLE_NAME)
        locate?.apply {
            addRealmObjectField(Locate.FIELD_LOCATION_GROUP, locationGroup)
        }

        val deploymentLocation = realm.schema.get(DeploymentLocation.TABLE_NAME)
        deploymentLocation?.apply {
            addRealmObjectField("locationGroup", locationGroup)
        }

        // Delete field first to avoid ref error
        // Remove fields that were not used in AudioMoth version
        val edgeDeployment = realm.schema.get("EdgeDeployment")
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
        val edgeDeployment = realm.schema.get("EdgeDeployment")
        edgeDeployment?.apply {
            addRealmListField("passedChecks", Int::class.java)
                .setNullable("passedChecks", true)
        }
    }

    private fun migrateToV6(realm: DynamicRealm) {
        val deployment = realm.schema.get("GuardianDeployment")
        deployment?.apply {
            addField(Deployment.FIELD_UPDATED_AT, Date::class.java)
        }
    }

    private fun migrateToV7(realm: DynamicRealm) {
        val locationGroups = realm.schema.get(Project.TABLE_NAME)
        locationGroups?.apply {
            setNullable(Project.PROJECT_NAME, true)
            setNullable(Project.PROJECT_COLOR, true)
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

    private fun migrateToV9(realm: DynamicRealm) {

        val project = realm.schema.get(LocationGroup.TABLE_NAME)
        project?.apply {
            renameField("serverId", LocationGroup.FIELD_CORE_ID)
            renameField("group", LocationGroup.FIELD_NAME)
        }

        val stream = realm.schema.get(DeploymentLocation.TABLE_NAME)
        stream?.apply {
            renameField("locationGroup", DeploymentLocation.FIELD_PROJECT)
            addField(DeploymentLocation.FIELD_CORE_ID, String::class.java)
        }

        val edgeDeployment = realm.schema.get("EdgeDeployment")
        edgeDeployment?.apply {
            renameField("location", "stream")
            renameField("deploymentId", "deploymentKey")
        }

        val guardianDeployment = realm.schema.get("GuardianDeployment")
        guardianDeployment?.apply {
            renameField("location", Deployment.FIELD_STREAM)
        }
    }

    private fun migrateToV10(realm: DynamicRealm) {
        val edgeDeployment = realm.schema.get("EdgeDeployment")
        edgeDeployment?.apply {
            addField("isActive", Boolean::class.java)
        }
    }

    private fun migrateToV11(realm: DynamicRealm) {
        val locate = realm.schema.get(Locate.TABLE_NAME)
        locate?.apply {
            addField("updatedAt", Date::class.java)
        }
    }

    private fun migrateToV12(realm: DynamicRealm) {
        val locate = realm.schema.get(Locate.TABLE_NAME)
        locate?.apply {
            val hasDeletedAt = this.hasField(Locate.FIELD_DELETED_AT)
            if (hasDeletedAt) {
                removeField(Locate.FIELD_DELETED_AT)
            }
        }
    }

    private fun migrateToV13(realm: DynamicRealm) {

        val coordinate = realm.schema.create(Coordinate.TABLE_NAME)
        coordinate.apply {
            addField(Coordinate.COORDINATE_LATITUDE, Double::class.java)
            addField(Coordinate.COORDINATE_LONGITUDE, Double::class.java)
            addField(Coordinate.COORDINATE_ALTITUDE, Double::class.java)
        }

        val tracking = realm.schema.create(Tracking.TABLE_NAME)
        tracking.apply {
            addField(
                Tracking.TRACKING_ID,
                Int::class.java,
                FieldAttribute.PRIMARY_KEY
            )
            addField(Tracking.TRACKING_START_AT, Date::class.java)
                .setNullable(Tracking.TRACKING_START_AT, false)
            addField(Tracking.TRACKING_STOP_AT, Date::class.java)
            addRealmListField(Tracking.TRACKING_POINTS, coordinate)
        }

        val trackingFile = realm.schema.create(TrackingFile.TABLE_NAME)
        trackingFile.apply {
            addField(
                TrackingFile.FIELD_ID,
                Int::class.java,
                FieldAttribute.PRIMARY_KEY
            )
            addField(TrackingFile.FIELD_DEPLOYMENT_ID, Int::class.java)
            addField(TrackingFile.FIELD_DEPLOYMENT_SERVER_ID, String::class.java)
            addField(TrackingFile.FIELD_LOCAL_PATH, String::class.java)
                .setNullable(TrackingFile.FIELD_LOCAL_PATH, false)
            addField(TrackingFile.FIELD_REMOTE_PATH, String::class.java)
            addField(TrackingFile.FIELD_SYNC_STATE, Int::class.java)
            addField(TrackingFile.FIELD_DEVICE, String::class.java)
                .setNullable(TrackingFile.FIELD_DEVICE, false)
            addField(TrackingFile.FIELD_SITE_ID, Int::class.java)
            addField(TrackingFile.FIELD_SITE_SERVER_ID, String::class.java)
        }
    }

    private fun migrateToV14(realm: DynamicRealm) {
        val trackingFile = realm.schema.get(TrackingFile.TABLE_NAME)
        trackingFile?.apply {
            removeField(TrackingFile.FIELD_DEVICE)
        }

        val guardianDp = realm.schema.get("GuardianDeployment")
        guardianDp?.apply {
            addField("deploymentKey", String::class.java)
                .setRequired("deploymentKey", true)
            addField("isActive", Boolean::class.java)
        }

        val profile = realm.schema.get("GuardianProfile")
        val diagnostic = realm.schema.get("DiagnosticInfo")
        profile?.let {
            realm.schema.remove("GuardianProfile")
        }
        diagnostic?.let {
            realm.schema.remove("DiagnosticInfo")
        }
    }

    private fun migrateToV15(realm: DynamicRealm) {
        val locationGroups = realm.schema.get("LocationGroups")
        locationGroups?.apply {
            className = "Project"
        }
    }

    private fun migrateToV16(realm: DynamicRealm) {
        val deployment = realm.schema.get("GuardianDeployment")
        deployment?.apply {
            addField(Deployment.FIELD_DELETED_AT, Date::class.java)
            addRealmListField(Deployment.FIELD_PASSED_CHECKS, Int::class.java)
                .setNullable(Deployment.FIELD_PASSED_CHECKS, true)
        }

        val project = realm.schema.get("Project")
        project?.apply {
            addField(Project.PROJECT_MAX_LATITUDE, Double::class.java)
            addField(Project.PROJECT_MAX_LONGITUDE, Double::class.java)
            addField(Project.PROJECT_MIN_LATITUDE, Double::class.java)
            addField(Project.PROJECT_MIN_LONGITUDE, Double::class.java)
            addField(Project.PROJECT_OFFLINE_MAP_STATE, String::class.java)
            addField(Project.PROJECT_PERMISSIONS, String::class.java)
                .setRequired(Project.PROJECT_PERMISSIONS, true)
        }
    }

    private fun migrateToV17(realm: DynamicRealm) {
        val deployment = realm.schema.get("GuardianDeployment")
        deployment?.apply {
            className = Deployment.TABLE_NAME
        }
    }

    private fun migrateToV18(realm: DynamicRealm) {
        val locate = realm.schema.get(Locate.TABLE_NAME)
        locate?.apply {
            val lastDeploymentId = this.hasField("lastDeploymentId")
            if (lastDeploymentId) {
                removeField("lastDeploymentId")
            }
            val lastDeploymentServerId = this.hasField("lastDeploymentServerId")
            if (lastDeploymentServerId) {
                removeField("lastDeploymentServerId")
            }
            val lastGuardianDeploymentId = this.hasField("lastGuardianDeploymentId")
            if (lastGuardianDeploymentId) {
                removeField("lastGuardianDeploymentId")
            }
            val lastGuardianDeploymentServerId = this.hasField("lastGuardianDeploymentServerId")
            if (lastGuardianDeploymentServerId) {
                removeField("lastGuardianDeploymentServerId")
            }
        }
    }

    override fun hashCode(): Int {
        return 1
    }

    override fun equals(other: Any?): Boolean {
        return other.hashCode() == hashCode()
    }
}
