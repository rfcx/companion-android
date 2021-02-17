package org.rfcx.companion.util

import android.content.Context
import io.realm.Realm
import org.rfcx.companion.view.LoginActivity
import org.rfcx.companion.view.profile.coordinates.CoordinatesActivity.Companion.DD_FORMAT

fun Context?.getUserNickname(): String {
    val preferences = this?.let { Preferences.getInstance(it) }
    val nickname = preferences?.getString(Preferences.NICKNAME)
    return if (nickname != null && nickname.isNotEmpty()) nickname.capitalize() else "Ranger"
}

fun Context.getDefaultSiteName(): String {
    val defaultSiteName = Preferences.getInstance(this).getString(Preferences.DEFAULT_SITE, "")
    return defaultSiteName.capitalize()
}

fun Context.getEmailUser(): String {
    val preferences = this.let { Preferences.getInstance(it) }
    val email = preferences.getString(Preferences.EMAIL)
    return email ?: getUserNickname()
}

fun Context.getIdToken(): String? {
    val preferences = this.let { Preferences.getInstance(it) }
    return preferences.getString(Preferences.ID_TOKEN)
}

fun Context.logout() {
    Preferences.getInstance(this).clear()
    Realm.getInstance(RealmHelper.migrationConfig()).use { realm ->
        realm.executeTransactionAsync({ bgRealm ->
            bgRealm.deleteAll()
        }, {
            realm.close()
            LoginActivity.startActivity(this)
        }, {
            realm.close()
        })
    }
}

fun Context?.getCoordinatesFormat(): String? {
    val preferences = this?.let { Preferences.getInstance(it) }
    return preferences?.getString(Preferences.COORDINATES_FORMAT, DD_FORMAT)
}
