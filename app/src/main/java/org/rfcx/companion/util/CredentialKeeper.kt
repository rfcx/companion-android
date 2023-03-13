package org.rfcx.companion.util

import android.content.Context
import org.rfcx.companion.entity.UserAuthResponse

class CredentialKeeper(val context: Context) {

    fun save(user: UserAuthResponse) {
        val preferences = Preferences.getInstance(context)
        // Required
        preferences.putString(Preferences.USER_GUID, user.guid)
        preferences.putString(Preferences.ID_TOKEN, user.idToken)
        preferences.putLong(Preferences.TOKEN_EXPIRES_AT, user.expires * 1000)

        // Optional
        if (user.accessToken != null) {
            preferences.putString(Preferences.ACCESS_TOKEN, user.accessToken)
        }
        if (user.refreshToken != null) {
            preferences.putString(Preferences.REFRESH_TOKEN, user.refreshToken)
        }
        if (user.email != null) {
            preferences.putString(Preferences.EMAIL, user.email)
        }
        if (user.nickname != null) {
            preferences.putString(Preferences.NICKNAME, user.nickname)
        }
        if (user.picture != null) {
            preferences.putString(Preferences.IMAGE_PROFILE, user.picture)
        }
        preferences.putStringSet(Preferences.ROLES, user.roles)
        preferences.putStringSet(Preferences.ACCESSIBLE_SITES, user.accessibleSites)
        if (user.defaultSite != null) {
            preferences.putString(Preferences.DEFAULT_SITE, user.defaultSite)
        }
    }

    fun clear() {
        val preferences = Preferences.getInstance(context)
        preferences.clear()
    }

    fun hasValidCredentials(): Boolean {
        val preferences = Preferences.getInstance(context)
        val idToken = if (!isTokenExpired()) preferences.getString(Preferences.ID_TOKEN) else null
        val userFirebaseUid = preferences.getString(Preferences.USER_FIREBASE_UID)
        return idToken != null && userFirebaseUid != null
    }

    private fun isTokenExpired(): Boolean {
        val preferences = Preferences.getInstance(context)
        return System.currentTimeMillis() > preferences.getLong(Preferences.TOKEN_EXPIRES_AT, 0)
    }
}
