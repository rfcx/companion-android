package org.rfcx.companion.view.profile.guardianregistration

import android.view.Gravity
import androidx.transition.Slide
import androidx.transition.TransitionSet

class RegisterBannerTransition : TransitionSet() {
    init {
        init()
    }

    private fun init() {
        ordering = ORDERING_TOGETHER

        addTransition(Slide(Gravity.TOP))
    }
}
