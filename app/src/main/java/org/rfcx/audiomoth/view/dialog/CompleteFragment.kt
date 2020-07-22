package org.rfcx.audiomoth.view.dialog

import android.animation.Animator
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import kotlinx.android.synthetic.main.fragment_complete.*
import org.rfcx.audiomoth.R

class CompleteFragment : DialogFragment() {
    private var completeListener: CompleteListener? = null

    override fun onAttach(context: Context) {
        super.onAttach(context)
        completeListener = (context as CompleteListener)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_complete, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        dialog?.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        animationView.addAnimatorListener(object : Animator.AnimatorListener {
            override fun onAnimationRepeat(animation: Animator?) {}

            override fun onAnimationEnd(animation: Animator?) {
                completeListener?.onAnimationEnd()
            }

            override fun onAnimationCancel(animation: Animator?) {}

            override fun onAnimationStart(animation: Animator?) {}
        })
    }

    companion object {
        const val tag = "CompleteFragment"

        @JvmStatic
        fun newInstance() = CompleteFragment()
    }
}
