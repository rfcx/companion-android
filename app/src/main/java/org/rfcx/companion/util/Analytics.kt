package org.rfcx.companion.util

import android.app.Activity
import android.content.Context
import android.os.Bundle
import com.google.firebase.analytics.FirebaseAnalytics
import org.rfcx.companion.entity.Event
import org.rfcx.companion.entity.Screen

class Analytics(context: Context) {
    private var firebaseAnalytics = FirebaseAnalytics.getInstance(context)
    private var context: Context? = context

    // region track screen
    fun trackScreen(screen: Screen) {
        firebaseAnalytics.setCurrentScreen(context as Activity, screen.id, null)
    }

    // region track event
    private fun trackEvent(eventName: String, params: Bundle) {
        val preferences = context?.let { Preferences.getInstance(it) }
        val user = preferences?.getString(Preferences.USER_FIREBASE_UID, "")
        firebaseAnalytics.setUserProperty(USER_UID, user)
        firebaseAnalytics.logEvent(eventName, params)
    }

    fun trackLoginEvent(type: String, status: String) {
        val bundle = Bundle()
        bundle.putString(LOGIN_TYPE, type)
        bundle.putString(STATUS, status)
        trackEvent(FirebaseAnalytics.Event.LOGIN, bundle)
    }

    fun trackLogoutEvent() {
        val bundle = Bundle()
        trackEvent(Event.LOGOUT.id, bundle)
    }

    fun trackClickPinEvent(status: String) {
        val bundle = Bundle()
        bundle.putString(STATUS, status)
        trackEvent(Event.CLICK_PIN.id, bundle)
    }

    fun trackSeeDetailEvent() {
        val bundle = Bundle()
        trackEvent(Event.SEE_DETAIL.id, bundle)
    }

    fun trackConnectCreateDeploymentEvent() {
        val bundle = Bundle()
        trackEvent(Event.CONNECT_CREATE_DEPLOYMENT.id, bundle)
    }

    fun trackDeleteDeploymentEvent(status: String) {
        val bundle = Bundle()
        bundle.putString(STATUS, status)
        trackEvent(Event.DELETE_DEPLOYMENT.id, bundle)
    }

    fun trackChangeCoordinatesEvent(format: String) {
        val bundle = Bundle()
        bundle.putString(FirebaseAnalytics.Param.ITEM_NAME, format)
        trackEvent(Event.CHANGE_COORDINATES.id, bundle)
    }

    fun trackCreateNewGroupEvent() {
        val bundle = Bundle()
        trackEvent(Event.CREATE_NEW_GROUP.id, bundle)
    }

    fun trackSaveNewGroupEvent() {
        val bundle = Bundle()
        trackEvent(Event.SAVE_NEW_GROUP.id, bundle)
    }
    
    fun trackDeleteLocationGroupEvent(status: String) {
        val bundle = Bundle()
        bundle.putString(STATUS, status)
        trackEvent(Event.DELETE_LOCATION_GROUP.id, bundle)
    }

    fun trackChangeThemeEvent(theme: String) {
        val bundle = Bundle()
        bundle.putString(FirebaseAnalytics.Param.ITEM_NAME, theme)
        trackEvent(Event.CHANGE_THEME.id, bundle)
    }

    fun trackSendFeedbackEvent(status: String) {
        val bundle = Bundle()
        bundle.putString(STATUS, status)
        trackEvent(Event.SEND_FEEDBACK.id, bundle)
    }

    fun trackAddFeedbackImagesEvent() {
        val bundle = Bundle()
        trackEvent(Event.ADD_FEEDBACK_IMAGES.id, bundle)
    }

    fun trackSearchLocationEvent() {
        val bundle = Bundle()
        trackEvent(Event.SEARCH_LOCATION.id, bundle)
    }

    fun trackSelectLocationEvent() {
        val bundle = Bundle()
        trackEvent(Event.SELECT_LOCATION.id, bundle)
    }

    fun trackEditLocationEvent() {
        val bundle = Bundle()
        trackEvent(Event.EDIT_LOCATION.id, bundle)
    }

    companion object {
        const val USER_UID = "user_uid"
        const val LOGIN_TYPE = "login_type"
        const val STATUS = "status"
    }
}
