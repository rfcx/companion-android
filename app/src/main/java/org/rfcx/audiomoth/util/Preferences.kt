package org.rfcx.audiomoth.util

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

}