package org.rfcx.companion.view.dialog

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.NumberPicker
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import kotlinx.android.synthetic.main.dialog_month_year_picker.*
import org.rfcx.companion.R
import java.util.*

class MonthYearPickerDialog(val callback: OnPickListener) : DialogFragment() {

    companion object {
        private const val ARG_LATEST = "ARG_LATEST"
        private const val ARG_SELECTED_MONTH = "ARG_SELECTED_MONTH"
        private const val ARG_SELECTED_YEAR = "ARG_SELECTED_YEAR"
        private const val ARG_MIN_YEAR = "ARG_MIN_YEAR"
        private const val ARG_MAX_YEAR = "ARG_MAX_YEAR"

        fun newInstance(latest: Long, selectedMonth: Int, selectedYear: Int, minYear: Int, maxYear: Int, callback: OnPickListener) =
            MonthYearPickerDialog(callback).apply {
                arguments = Bundle().apply {
                    putLong(ARG_LATEST, latest)
                    putInt(ARG_SELECTED_MONTH, selectedMonth)
                    putInt(ARG_SELECTED_YEAR, selectedYear)
                    putInt(ARG_MIN_YEAR, minYear)
                    putInt(ARG_MAX_YEAR, maxYear)
                }
            }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val cal = Calendar.getInstance()
        val latest = arguments?.getLong(ARG_LATEST) ?: System.currentTimeMillis()
        cal.time = Date(latest)

        val selectedMonth = arguments?.getInt(ARG_SELECTED_MONTH) ?: cal.get(Calendar.MONTH)
        val selectedYear = arguments?.getInt(ARG_SELECTED_YEAR) ?: cal.get(Calendar.YEAR)

        val minYear = arguments?.getInt(ARG_MIN_YEAR) ?: cal.get(Calendar.YEAR)
        val maxYear = arguments?.getInt(ARG_MAX_YEAR) ?: cal.get(Calendar.YEAR)

        val inflater = requireActivity().layoutInflater
        val view = inflater.inflate(R.layout.dialog_month_year_picker, null)
        val monthView = view.findViewById<NumberPicker>(R.id.pickerMonth)
        monthView.apply {
            minValue = 0
            maxValue = 11
            value = selectedMonth
            displayedValues = arrayOf("Jan","Feb","Mar","Apr","May","Jun","Jul",
                "Aug","Sep","Oct","Nov","Dec")
        }

        val yearView = view.findViewById<NumberPicker>(R.id.pickerYear)
        yearView.apply {
            minValue = minYear
            maxValue = maxYear
            value = selectedYear
        }

        return AlertDialog.Builder(requireContext(), R.style.BaseAlertDialog)
            .setTitle(R.string.select_month_year)
            .setView(view)
            .setPositiveButton(R.string.ok) { _, _ -> callback.onPick(monthView.value, yearView.value) }
            .setNegativeButton(R.string.cancel) { _, _ -> dialog?.cancel() }
            .create()
    }

    interface OnPickListener {
        fun onPick(month: Int, year: Int)
    }
}
