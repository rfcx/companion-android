package org.rfcx.companion.util

import android.content.Context
import android.os.Bundle
import com.posthog.PostHog
import org.rfcx.companion.entity.Event
import org.rfcx.companion.entity.Screen

// Product analytics is self-hosted PostHog (track.rfcx.org) as of 2026-07-15,
// replacing Firebase Analytics. Crashlytics (error logging) stays on Firebase.
// The public API of this class (trackScreen + the trackXEvent methods) and every
// call site are unchanged; only the two backing methods below send to PostHog
// instead of Firebase. Firebase event ids + param keys are preserved verbatim so
// the analytics taxonomy is byte-identical — only the destination changed.
class Analytics(context: Context) {
    private var context: Context? = context

    // Convert the existing Firebase Bundle params to the Map PostHog expects.
    private fun Bundle.toMap(): Map<String, Any> {
        val map = HashMap<String, Any>()
        for (key in keySet()) {
            @Suppress("DEPRECATION")
            get(key)?.let { map[key] = it }
        }
        return map
    }

    // region track screen
    fun trackScreen(screen: Screen) {
        // Manual screen capture (autocapture is off). PostHog's screen event.
        PostHog.screen(screen.id)
    }

    // region track event
    private fun trackEvent(eventName: String, params: Bundle) {
        val preferences = context?.let { Preferences.getInstance(it) }
        // Identity = the user's account email (operator decision 2026-07-15),
        // shared across all rfcx clients.
        val email = preferences?.getString(Preferences.EMAIL, "")
        if (!email.isNullOrEmpty()) {
            PostHog.identify(email)
        }
        PostHog.capture(eventName, properties = params.toMap())
    }

    fun trackLoginEvent(type: String, status: String) {
        val bundle = Bundle()
        bundle.putString(LOGIN_TYPE, type)
        bundle.putString(STATUS, status)
        trackEvent(EVENT_LOGIN, bundle)
    }

    fun trackLogoutEvent() {
        val bundle = Bundle()
        trackEvent(Event.LOGOUT.id, bundle)
    }

    fun trackClickPinEvent() {
        val bundle = Bundle()
        trackEvent(Event.CLICK_PIN.id, bundle)
    }

    fun trackSeeDetailEvent() {
        val bundle = Bundle()
        trackEvent(Event.SEE_DETAIL.id, bundle)
    }

    fun trackDeleteDeploymentEvent(status: String) {
        val bundle = Bundle()
        bundle.putString(STATUS, status)
        trackEvent(Event.DELETE_DEPLOYMENT.id, bundle)
    }

    fun trackChangeCoordinatesEvent(format: String) {
        val bundle = Bundle()
        bundle.putString(ITEM_NAME, format)
        trackEvent(Event.CHANGE_COORDINATES.id, bundle)
    }
    fun trackChangeThemeEvent(theme: String) {
        val bundle = Bundle()
        bundle.putString(ITEM_NAME, theme)
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

    fun trackSelectLocationEvent() {
        val bundle = Bundle()
        trackEvent(Event.SELECT_LOCATION.id, bundle)
    }

    fun trackEditLocationEvent() {
        val bundle = Bundle()
        trackEvent(Event.EDIT_LOCATION.id, bundle)
    }

    fun trackChangeLocationEvent(page: String) {
        val bundle = Bundle()
        bundle.putString(FROM_PAGE, page)
        trackEvent(Event.CHANGE_LOCATION.id, bundle)
    }

    fun trackChangeLocationGroupEvent(page: String) {
        val bundle = Bundle()
        bundle.putString(FROM_PAGE, page)
        trackEvent(Event.CHANGE_LOCATION_GROUP.id, bundle)
    }

    fun trackSaveLocationEvent(page: String) {
        val bundle = Bundle()
        bundle.putString(FROM_PAGE, page)
        trackEvent(Event.SAVE_LOCATION.id, bundle)
    }

    fun trackPlayToneEvent() {
        val bundle = Bundle()
        trackEvent(Event.PLAY_TONE.id, bundle)
    }

    fun trackPlayToneCompletedEvent() {
        val bundle = Bundle()
        trackEvent(Event.PLAY_TONE_COMPLETED.id, bundle)
    }

    fun trackRetryPlayToneEvent() {
        val bundle = Bundle()
        trackEvent(Event.RETRY_PLAY_TONE.id, bundle)
    }

    fun trackPlaySyncToneEvent() {
        val bundle = Bundle()
        trackEvent(Event.PLAY_SYNC_TONE.id, bundle)
    }

    fun trackPlaySyncToneCompletedEvent() {
        val bundle = Bundle()
        trackEvent(Event.PLAY_SYNC_TONE_COMPLETED.id, bundle)
    }

    fun trackAddDeploymentImageEvent(device: String) {
        val bundle = Bundle()
        bundle.putString(DEVICE, device)
        trackEvent(Event.ADD_DEPLOYMENT_IMAGE.id, bundle)
    }

    fun trackCreateAudiomothDeploymentEvent() {
        val bundle = Bundle()
        trackEvent(Event.CREATE_AUDIOMOTH_DEPLOYMENT.id, bundle)
    }

    fun trackCreateSongMeterDeploymentEvent() {
        val bundle = Bundle()
        trackEvent(Event.CREATE_SONGMETER_DEPLOYMENT.id, bundle)
    }

    companion object {
        const val USER_UID = "user_uid"
        const val LOGIN_TYPE = "login_type"
        const val STATUS = "status"
        const val FROM_PAGE = "from_page"
        const val DEVICE = "device"
        // Firebase reserved names, preserved verbatim as the destination changed
        // to PostHog (so the taxonomy is byte-identical to the Firebase era).
        const val EVENT_LOGIN = "login"
        const val ITEM_NAME = "item_name"
    }
}
