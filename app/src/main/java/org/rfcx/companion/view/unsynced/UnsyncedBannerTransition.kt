package org.rfcx.companion.view.unsynced

import android.view.Gravity
import androidx.transition.Slide
import androidx.transition.TransitionSet

class UnsyncedBannerTransition : TransitionSet() {
    init {
        init()
    }

    private fun init() {
        ordering = ORDERING_TOGETHER

        addTransition(Slide(Gravity.TOP))
    }
}
