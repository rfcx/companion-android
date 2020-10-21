package org.rfcx.companion.util

import android.app.Activity
import android.content.Context
import com.google.firebase.analytics.FirebaseAnalytics
import org.rfcx.companion.entity.Screen

class Analytics(context: Context) {
    private var firebaseAnalytics = FirebaseAnalytics.getInstance(context)
    private var context: Context? = context

    // region track screen
    fun trackScreen(screen: Screen) {
        firebaseAnalytics.setCurrentScreen(context as Activity, screen.id, null)
    }
}
