package org.rfcx.companion.view.dialog

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import kotlinx.android.synthetic.main.fragment_wifi_lost_dialog.*
import kotlinx.android.synthetic.main.number_picker_dialog.*
import org.rfcx.companion.R

class NumberPickerDialog : DialogFragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.number_picker_dialog, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        dialog?.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        numberPicker.maxValue = 30
        numberPicker.minValue = 0
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
        fun newInstance() = NumberPickerDialog()
    }
}
