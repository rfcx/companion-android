package org.rfcx.companion.view.dialog

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import kotlinx.android.synthetic.main.fragment_photo_guideline.*
import org.rfcx.companion.R

class PhotoGuidelineDialogFragment(private val guidelineButtonClickListener: GuidelineButtonClickListener) : DialogFragment() {

    override fun onStart() {
        super.onStart()
        val dialog = dialog
        if (dialog != null) {
            val width = ViewGroup.LayoutParams.MATCH_PARENT
            val height = ViewGroup.LayoutParams.MATCH_PARENT
            dialog.window!!.setLayout(width, height)
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_photo_guideline, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        continueButton.setOnClickListener {
            dismiss()
            guidelineButtonClickListener.onContinueClick()
        }
    }

    companion object {
        fun newInstance(callback: GuidelineButtonClickListener) = PhotoGuidelineDialogFragment(callback)
    }
}

interface GuidelineButtonClickListener {
    fun onContinueClick()
}
