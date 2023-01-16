package org.rfcx.companion.view.dialog

import androidx.fragment.app.DialogFragment
import java.util.*

class MonthYearPickerDialog(val date: Date = Date()) : DialogFragment() {

    companion object {
        private const val MAX_YEAR = 2099
    }

    private lateinit var binding: DialogMonthYearPickerBinding

    private var listener: OnDateSetListener? = null

    fun setListener(listener: OnDateSetListener?) {
        this.listener = listener
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        binding = DialogMonthYearPickerBinding.inflate(requireActivity().layoutInflater)
        val cal: Calendar = Calendar.getInstance().apply { time = date }

        binding.pickerMonth.run {
            minValue = 0
            maxValue = 11
            value = cal.get(Calendar.MONTH)
            displayedValues = arrayOf("Jan","Feb","Mar","Apr","May","June","July",
                "Aug","Sep","Oct","Nov","Dec")
        }

        binding.pickerYear.run {
            val year = cal.get(Calendar.YEAR)
            minValue = year
            maxValue = MAX_YEAR
            value = year
        }

        return AlertDialog.Builder(requireContext())
            .setTitle("Please Select View Month")
            .setView(binding.root)
            .setPositiveButton("Ok") { _, _ -> listener?.onDateSet(null, binding.pickerYear.value, binding.pickerMonth.value, 1) }
            .setNegativeButton("Cancel") { _, _ -> dialog?.cancel() }
            .create()
    }
}
