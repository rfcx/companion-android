package org.rfcx.companion.view.dialog

import android.animation.Animator
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import org.rfcx.companion.databinding.FragmentCompleteBinding

class CompleteFragment : DialogFragment() {
    private var completeListener: CompleteListener? = null
    private var _binding: FragmentCompleteBinding? = null
    private val binding get() = _binding!!

    override fun onAttach(context: Context) {
        super.onAttach(context)
        completeListener = (context as CompleteListener)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentCompleteBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        dialog?.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        binding.animationView.addAnimatorListener(object : Animator.AnimatorListener {
            override fun onAnimationRepeat(animation: Animator) {}

            override fun onAnimationEnd(animation: Animator) {
                completeListener?.onAnimationEnd()
            }

            override fun onAnimationCancel(animation: Animator) {}

            override fun onAnimationStart(animation: Animator) {}
        })
    }

    companion object {
        const val tag = "CompleteFragment"

        @JvmStatic
        fun newInstance() = CompleteFragment()
    }
}
