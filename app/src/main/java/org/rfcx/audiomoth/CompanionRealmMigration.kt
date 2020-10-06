package org.rfcx.audiomoth

import io.realm.DynamicRealm
import io.realm.FieldAttribute
import io.realm.RealmMigration
import org.rfcx.audiomoth.entity.EdgeConfiguration
import org.rfcx.audiomoth.entity.EdgeDeployment
import org.rfcx.audiomoth.entity.Locate
import org.rfcx.audiomoth.entity.LocationGroups
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

        val locate = realm.schema.get(Locate.TABLE_NAME)
        locate?.apply {
            addField(Locate.FIELD_GROUP, String::class.java)
            addField(Locate.FIELD_COLOR, String::class.java)
            addField(Locate.FIELD_GROUP_ID, Int::class.java)
                .setNullable(Locate.FIELD_GROUP_ID, true)
            addField(Locate.FIELD_GROUP_SERVER_ID, String::class.java)
        }
    }

    override fun hashCode(): Int {
        return 1
    }

    override fun equals(other: Any?): Boolean {
        return other.hashCode() == hashCode()
    }
}
