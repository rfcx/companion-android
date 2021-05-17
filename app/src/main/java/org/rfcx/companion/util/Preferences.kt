package org.rfcx.companion.util

import android.content.Context
import android.content.SharedPreferences

class Preferences(context: Context) {
    var sharedPreferences: SharedPreferences

    companion object {
        @Volatile
        private var INSTANCE: Preferences? = null

        fun getInstance(context: Context): Preferences =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Preferences(context).also { INSTANCE = it }
            }

        private const val PREFERENCES_NAME = "Rfcx.Ranger"
        private const val PREFIX = "org.rfcx.ranger:"

        const val ID_TOKEN = "${PREFIX}ID_TOKEN"
        const val USER_GUID = "${PREFIX}USER_GUID"
        const val ACCESS_TOKEN = "${PREFIX}ACCESS_TOKEN"
        const val REFRESH_TOKEN = "${PREFIX}REFRESH_TOKEN"
        const val EMAIL = "${PREFIX}EMAIL"
        const val NICKNAME = "${PREFIX}NICKNAME"
        const val IMAGE_PROFILE = "${PREFIX}IMAGE_PROFILE"
        const val ROLES = "${PREFIX}ROLES"
        const val ACCESSIBLE_SITES = "${PREFIX}ACCESSIBLE_SITES"
        const val DEFAULT_SITE = "${PREFIX}DEFAULT_SITE"
        const val IS_FIRST_TIME = "${PREFIX}IS_FIRST_TIME"
        const val COORDINATES_FORMAT = "${PREFIX}COORDINATES_FORMAT"
        const val USER_FIREBASE_UID = "${PREFIX}USER_FIREBASE_UID"
        const val DISPLAY_THEME = "${PREFIX}DISPLAY_THEME"
        const val GROUP = "${PREFIX}GROUP"
        const val SELECTED_PROJECT = "${PREFIX}SELECTED_PROJECT"
        const val TOKEN_EXPIRES_AT = "${PREFIX}TOKEN_EXPIRES_AT"
        const val ENABLE_LOCATION_TRACKING = "${PREFIX}ENABLE_LOCATION_TRACKING"
        const val ON_DUTY = "${PREFIX}ON_DUTY"
        const val ON_DUTY_LAST_OPEN = "${PREFIX}ON_DUTY_LAST_OPEN"
        const val LASTEST_GET_LOCATION_TIME = "${PREFIX}LASTEST_GET_LOCATION_TIME"
        const val LAST_LATITUDE = "${PREFIX}LAST_LATITUDE"
        const val LAST_LONGITUDE = "${PREFIX}LAST_LONGITUDE"
    }

    init {
        sharedPreferences =
            context.applicationContext.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
    }

    fun getString(key: String, defValue: String): String {
        return sharedPreferences.getString(key, defValue) ?: defValue
    }

    fun getString(key: String): String? {
        return sharedPreferences.getString(key, null)
    }

    fun getBoolean(key: String, defaultValue: Boolean = false): Boolean {
        return sharedPreferences.getBoolean(key, defaultValue)
    }

    fun getLong(key: String, defValue: Long): Long {
        return sharedPreferences.getLong(key, defValue)
    }

    fun putLong(key: String, long: Long) {
        sharedPreferences.edit().putLong(key, long).apply()
    }


    fun getFloat(key: String): Float {
        return sharedPreferences.getFloat(key, 0.0F)
    }

    fun putFloat(key: String, float: Float) {
        sharedPreferences.edit().putFloat(key, float).apply()
    }

    fun putBoolean(key: String, value: Boolean) {
        sharedPreferences.edit().putBoolean(key, value).apply()
    }

    fun putStringSet(key: String, value: Set<String>) {
        sharedPreferences.edit().putStringSet(key, value).apply()
    }

    fun putString(key: String, value: String) {
        sharedPreferences.edit().putString(key, value).apply()
    }

    fun clear() {
        sharedPreferences.edit().clear().apply()
    }

    fun clearSelectedProject() {
        sharedPreferences.edit().remove(GROUP).apply()
    }
}
