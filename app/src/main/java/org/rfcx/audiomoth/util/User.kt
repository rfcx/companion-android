package org.rfcx.audiomoth.util

import android.content.Context

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

