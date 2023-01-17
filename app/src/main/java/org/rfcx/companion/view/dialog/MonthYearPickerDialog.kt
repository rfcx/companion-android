package org.rfcx.companion.view.dialog

import android.app.Dialog
import android.os.Bundle
import android.widget.NumberPicker
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import org.rfcx.companion.R
import java.util.*

class MonthYearPickerDialog(val callback: OnPickListener) : DialogFragment() {

    companion object {
        private const val ARG_LATEST = "ARG_LATEST"
        private const val ARG_SELECTED_MONTH = "ARG_SELECTED_MONTH"
        private const val ARG_SELECTED_YEAR = "ARG_SELECTED_YEAR"
        private const val ARG_AVAILABLE_YEAR_MONTH = "ARG_AVAILABLE_YEAR_MONTH"

        fun newInstance(
            latest: Long,
            selectedMonth: Int,
            selectedYear: Int,
            availableYearMonth: HashMap<Int, List<Int>>,
            callback: OnPickListener
        ) = MonthYearPickerDialog(callback).apply {
            arguments = Bundle().apply {
                putLong(ARG_LATEST, latest)
                putInt(ARG_SELECTED_MONTH, selectedMonth)
                putInt(ARG_SELECTED_YEAR, selectedYear)
                putSerializable(ARG_AVAILABLE_YEAR_MONTH, availableYearMonth)
            }
        }
    }

    private var displayedMonthValues = listOf<String>()
    private var displayedYearValues = listOf<String>()
    private var availableYearMonth = mapOf<Int, List<Int>>()

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val cal = Calendar.getInstance()
        val latest = arguments?.getLong(ARG_LATEST) ?: System.currentTimeMillis()
        cal.time = Date(latest)

        val selectedMonth = arguments?.getInt(ARG_SELECTED_MONTH) ?: cal.get(Calendar.MONTH)
        val selectedYear = arguments?.getInt(ARG_SELECTED_YEAR) ?: cal.get(Calendar.YEAR)

        availableYearMonth =
            arguments?.getSerializable(ARG_AVAILABLE_YEAR_MONTH) as Map<Int, List<Int>>

        displayedYearValues = availableYearMonth.keys.toList().sorted().map { it.toString() }
        val defaultMonthValues = arrayOf(
            "Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"
        )
        displayedMonthValues = defaultMonthValues.filterIndexed { index, _ ->
            availableYearMonth[selectedYear]!!.contains(index)
        }

        val inflater = requireActivity().layoutInflater
        val view = inflater.inflate(R.layout.dialog_month_year_picker, null)
        val monthView = view.findViewById<NumberPicker>(R.id.pickerMonth)
        monthView.apply {
            minValue = 0
            maxValue = availableYearMonth[selectedYear]!!.size - 1
            value = availableYearMonth[selectedYear]!!.indexOf(selectedMonth)
            displayedValues = displayedMonthValues.toTypedArray()
        }

        val yearView = view.findViewById<NumberPicker>(R.id.pickerYear)
        yearView.apply {
            minValue = 0
            maxValue = availableYearMonth.keys.size - 1
            value = displayedYearValues.indexOf(selectedYear.toString())
            displayedValues = displayedYearValues.toTypedArray()
        }

        yearView.setOnValueChangedListener { _, _, new ->
            monthView.apply {
                val indexYear = availableYearMonth.keys.toList().sorted()[new]
                displayedValues = null
                minValue = 0
                maxValue = availableYearMonth[indexYear]!!.size - 1
                value = 0
                displayedMonthValues = defaultMonthValues.filterIndexed { index, _ ->
                    availableYearMonth[indexYear]!!.contains(index)
                }
                displayedValues = displayedMonthValues.toTypedArray()
            }
        }

        return AlertDialog.Builder(requireContext(), R.style.BaseAlertDialog)
            .setTitle(R.string.select_month_year).setView(view)
            .setPositiveButton(R.string.ok) { _, _ ->
                val year = availableYearMonth.keys.toList().sorted()[yearView.value]
                val month = availableYearMonth[year]!![monthView.value]
                callback.onPick(
                    month, year
                )
            }.setNegativeButton(R.string.cancel) { _, _ -> dialog?.cancel() }.create()
    }

    interface OnPickListener {
        fun onPick(month: Int, year: Int)
    }
}
