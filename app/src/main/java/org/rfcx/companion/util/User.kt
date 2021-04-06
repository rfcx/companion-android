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
    return "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCIsImtpZCI6IlF6UTROakpHTkRVd01UTTJSVGREUmpRMVFqazROa001UVVFMU1URTJSREl5T1VJME9VRkVOQSJ9.eyJodHRwczovL3JmY3gub3JnL2FwcF9tZXRhZGF0YSI6eyJhdXRob3JpemF0aW9uIjp7InJvbGVzIjpbInJmY3hVc2VyIl19LCJndWlkIjoiYzE5MWM3YjgtN2QyZS00NzU2LThlYzctNTRhMmIxNmI2NGVkIiwibG9naW5zTnVtYmVyIjoxMn0sImh0dHBzOi8vcmZjeC5vcmcvdXNlcl9tZXRhZGF0YSI6eyJuYW1lIjoiS2VsdnluIE5vYSIsImdpdmVuX25hbWUiOiJLZWx2eW4iLCJmYW1pbHlfbmFtZSI6Ik5vYSIsImNvbnNlbnRHaXZlbkRhc2hib2FyZCI6InRydWUiLCJjb25zZW50R2l2ZW4iOiJ0cnVlIiwiY29uc2VudEdpdmVuQWNvdXN0aWNzRXhwbG9yZXIiOiJ0cnVlIiwiY29uc2VudEdpdmVuUmFuZ2VyQXBwIjoidHJ1ZSJ9LCJnaXZlbl9uYW1lIjoiS2VsdnluIiwiZmFtaWx5X25hbWUiOiJOb2EiLCJuaWNrbmFtZSI6ImtlbHZ5bi5ub2EiLCJuYW1lIjoiS2VsdnluIE5vYSIsInBpY3R1cmUiOiJodHRwczovL3MuZ3JhdmF0YXIuY29tL2F2YXRhci9mY2NjODZjNDAyNDAxYjJkZjQwODJhYWUwN2IyYWExND9zPTQ4MCZyPXBnJmQ9aHR0cHMlM0ElMkYlMkZjZG4uYXV0aDAuY29tJTJGYXZhdGFycyUyRmtuLnBuZyIsInVwZGF0ZWRfYXQiOiIyMDIxLTAzLTExVDAzOjI4OjUzLjAwOVoiLCJlbWFpbCI6ImtlbHZ5bi5ub2FAdXByLmVkdSIsImVtYWlsX3ZlcmlmaWVkIjp0cnVlLCJpc3MiOiJodHRwczovL2F1dGgucmZjeC5vcmcvIiwic3ViIjoiYXV0aDB8NjAzODJkZmRhMmJjNjQwMDZjODQ5MGQ3IiwiYXVkIjoiWTl6cXhhU3ROYWQ0ODZFbkpKZHVJY0Z0cm1ya0hwdlYiLCJpYXQiOjE2MTU0MzMzMzUsImV4cCI6MTYxODA2MzA4MX0.oOIgyRoCzA-R2uuBkqAEf-c_bPQEG4Sdw1_6S0veOR3u1xPvbJnkluIegGMMv6oHDa1Hq40hQ4VLb9Dw-iiHD8xa9Iw_AaW2Mrf0GdsTS8aOj1vk1I54LpPIAO-Y8B5l0zNElUmDOUgr3LMIrj2L6m4Jtbb4EMlBoj0l2LuSz4wmWmMFqiEUzfZqD_nPrIVRWXN5YkL3AjhZ5VEH4Zt6pM44MvXJhmsDoNzmzMXGX_BtVVSq1Vmm1jcXAw0j6sJXhwn5JXWALVXhXRApB-Fo5rIRO3Obx2zClZ5VvZgsEw_U5VMMXFW14_tCrcSCO3TyAIkb5QA-_LWEoeu4d_6r4w"
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
