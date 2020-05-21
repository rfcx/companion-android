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