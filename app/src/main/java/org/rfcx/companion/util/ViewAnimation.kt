package org.rfcx.companion.util

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.view.View

class ViewAnimation {
    fun rotateFab(view: View, rotate: Boolean): Boolean {
        view.animate().setDuration(200)
            .setListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator?) {
                    super.onAnimationEnd(animation)
                }
            })
            .rotation(if (rotate) 0f else 180f)
        return rotate
    }

    fun showIn(view: View) {
        view.visibility = View.VISIBLE
        view.alpha = 0f
        view.translationY = view.height.toFloat()
        view.animate()
            .setDuration(200)
            .translationY(0f)
            .setListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    super.onAnimationEnd(animation)
                }
            })
            .alpha(1f)
            .start()
    }

    fun showOut(view: View) {
        view.visibility = View.VISIBLE
        view.alpha = 1f
        view.translationY = 0f
        view.animate()
            .setDuration(200)
            .translationY(view.height.toFloat())
            .setListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    view.visibility = View.GONE
                    super.onAnimationEnd(animation)
                }
            }).alpha(0f)
            .start()
    }

    fun init(view: View) {
        view.visibility = View.GONE
        view.translationY = view.height.toFloat()
        view.alpha = 0f
    }
}
