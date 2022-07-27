package org.rfcx.companion.util

import com.google.firebase.crashlytics.FirebaseCrashlytics

class Crashlytics {
    private val firebaseCrashlytics = FirebaseCrashlytics.getInstance()

    fun setCustomKey(key: String, value: String) {
        firebaseCrashlytics.setCustomKey(key, value)
    }
}
