package org.rfcx.companion.view.dialog

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import kotlinx.android.synthetic.main.fragment_photo_guideline.*
import org.rfcx.companion.R

class PhotoGuidelineDialogFragment(private val guidelineButtonClickListener: GuidelineButtonClickListener) :
    DialogFragment() {

    private var guidelineText = ""
    private var photoId = ""

    override fun onStart() {
        super.onStart()
        val dialog = dialog
        if (dialog != null) {
            val width = ViewGroup.LayoutParams.MATCH_PARENT
            val height = ViewGroup.LayoutParams.MATCH_PARENT
            dialog.window!!.setLayout(width, height)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        guidelineText = arguments?.getString(ARG_TEXT) ?: ""
        photoId = arguments?.getString(ARG_PHOTO) ?: ""
        return inflater.inflate(R.layout.fragment_photo_guideline, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setExamplePhoto(photoId)
        setExampleText(guidelineText)

        takePhotoButton.setOnClickListener {
            dismiss()
            guidelineButtonClickListener.onTakePhotoClick()
        }

        choosePhotoButton.setOnClickListener {
            dismiss()
            guidelineButtonClickListener.onChoosePhotoClick()
        }
    }

    private fun setExamplePhoto(photoId: String) {
        if (photoId.isEmpty()) return

        val id = resources.getIdentifier(photoId, "drawable", requireContext().packageName)
        if (id == 0) return

        guidelineImage.setImageResource(resources.getIdentifier(photoId, "drawable", requireContext().packageName))
    }

    private fun setExampleText(text: String) {
        guidelineTextView.text = text.ifEmpty { getString(R.string.take_other) }
    }

    companion object {
        private const val ARG_TEXT = "ARG_TEXT"
        private const val ARG_PHOTO = "ARG_PHOTO"

        fun newInstance(
            callback: GuidelineButtonClickListener,
            guidelineText: String?,
            guidelinePhotoId: String?
        ): PhotoGuidelineDialogFragment {

            return PhotoGuidelineDialogFragment(callback).apply {
                arguments = Bundle().apply {
                    putString(ARG_TEXT, guidelineText)
                    putString(ARG_PHOTO, guidelinePhotoId)
                }
            }
        }
    }
}

interface GuidelineButtonClickListener {
    fun onTakePhotoClick()
    fun onChoosePhotoClick()
}
