package org.rfcx.companion.view.dialog

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import kotlinx.android.synthetic.main.number_picker_dialog.*
import org.rfcx.companion.R

class NumberPickerDialog(private val callback: NumberPickerButtonClickListener) : DialogFragment() {

    private var prefsNumberValue = 0

    override fun onStart() {
        super.onStart()
        dialog?.let {
            val width = ViewGroup.LayoutParams.MATCH_PARENT
            val height = ViewGroup.LayoutParams.MATCH_PARENT
            it.window!!.setLayout(width, height)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        prefsNumberValue = arguments?.getInt(ARG_VALUE) ?: 0
        return inflater.inflate(R.layout.number_picker_dialog, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        outsidePicker.setOnClickListener {
            dismissDialog()
        }

        cancelButton.setOnClickListener {
            dismissDialog()
        }

        nextButton.setOnClickListener {
            dismissDialog()
            callback.onNextClicked(numberPicker.value)
        }

        dialog?.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        numberPicker.maxValue = 30
        numberPicker.minValue = 0
        numberPicker.value = prefsNumberValue
    }

    private fun dismissDialog() {
        try {
            dismiss()
        } catch (e: Exception) {
            e.printStackTrace()
            dismissAllowingStateLoss()
        }
    }

    companion object {
        private const val ARG_VALUE = "ARG_VALUE"

        fun newInstance(number: Int, callback: NumberPickerButtonClickListener) =
            NumberPickerDialog(callback).apply {
                arguments = Bundle().apply {
                    putInt(ARG_VALUE, number)
                }
            }
    }
}

interface NumberPickerButtonClickListener {
    fun onNextClicked(number: Int)
}
