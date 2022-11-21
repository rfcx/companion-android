package org.rfcx.companion.widget

import android.content.Context
import android.os.Parcel
import android.os.Parcelable
import android.util.AttributeSet
import android.view.View
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.ViewCompat
import androidx.fragment.app.FragmentManager
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.timepicker.MaterialTimePicker
import com.google.android.material.timepicker.TimeFormat
import org.rfcx.companion.R

class StartStopTimePicker @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr), View.OnClickListener {

    var fragmentManager: FragmentManager? = null

    private var chipGroup: ChipGroup
    private var addChip: Chip

    var allowAdd: Boolean = true
        set(value) {
            field = value
            if (value) {
                addChip.visibility = View.VISIBLE
            } else {
                addChip.visibility = View.GONE
            }
        }
    var startTitle: String? = "Select start time"
    var stopTitle: String? = "Select stop time"

    private var tempStartTime: String = ""
    private var tempStopTime: String = ""
    var listOfTime = arrayListOf<String>()

    init {
        View.inflate(context, R.layout.widget_start_stop_timepicker, this)
        chipGroup = findViewById(R.id.startStopChipGroup)
        addChip = findViewById(R.id.addChip)
        allowAdd = true
        initAttrs(attrs)
        setupView()
    }

    private fun initAttrs(attrs: AttributeSet?) {
        if (attrs == null) return
        val typedArray = context.obtainStyledAttributes(attrs, R.styleable.StartStopTimePicker)

        startTitle = typedArray.getString(R.styleable.StartStopTimePicker_startTitle)
        stopTitle = typedArray.getString(R.styleable.StartStopTimePicker_stopTitle)
        allowAdd = typedArray.getBoolean(R.styleable.StartStopTimePicker_allowAdd, true)

        typedArray.recycle()
    }

    override fun onSaveInstanceState(): Parcelable {
        val superState = super.onSaveInstanceState()
        val saveState = StartStopTimePickerSaveState(superState)
        saveState.allowAdd = this.allowAdd
        return saveState
    }

    override fun onRestoreInstanceState(state: Parcelable?) {
        if (state !is StartStopTimePickerSaveState) {
            super.onRestoreInstanceState(state)
            return
        } else {
            super.onRestoreInstanceState(state.superState)
            this.allowAdd = state.allowAdd
        }
    }

    private fun setupView() {
        val startPicker = MaterialTimePicker.Builder()
            .setTimeFormat(TimeFormat.CLOCK_24H)
            .setHour(0)
            .setMinute(0)
            .setTitleText(startTitle)
            .build()

        val stopPicker = MaterialTimePicker.Builder()
            .setTimeFormat(TimeFormat.CLOCK_24H)
            .setHour(0)
            .setMinute(0)
            .setTitleText(stopTitle)
            .build()

        startPicker.addOnPositiveButtonClickListener {
            val hour =
                if (startPicker.hour.toString().length == 1) "0${startPicker.hour}" else startPicker.hour.toString()
            val minute =
                if (startPicker.minute.toString().length == 1) "0${startPicker.minute}" else startPicker.minute.toString()
            tempStartTime = "$hour:$minute"
            if (fragmentManager == null) return@addOnPositiveButtonClickListener
            stopPicker.show(fragmentManager!!, "StopTimePicker")
        }

        stopPicker.addOnPositiveButtonClickListener {
            val hour =
                if (stopPicker.hour.toString().length == 1) "0${stopPicker.hour}" else stopPicker.hour.toString()
            val minute =
                if (stopPicker.minute.toString().length == 1) "0${stopPicker.minute}" else stopPicker.minute.toString()
            tempStopTime = "$hour:$minute"
            val fullTime = "$tempStartTime-$tempStopTime"
            addTimeOff(fullTime)
        }

        addChip.setOnClickListener {
            if (fragmentManager == null) return@setOnClickListener
            startPicker.show(fragmentManager!!, "StartTimePicker")
        }
    }

    fun setTimes(times: List<String>?) {
        if (times == null) return
        listOfTime.clear()
        listOfTime.addAll(times)
        setChip(listOfTime, allowAdd)
    }

    private fun clearAllChips() {
        chipGroup.removeViews(1, chipGroup.childCount - 1)
    }

    private fun setChip(times: List<String>, isAddAllowed: Boolean) {
        clearAllChips()
        times.forEach {
            addChip(it, isAddAllowed)
        }
    }

    private fun addTimeOff(time: String) {
        if (listOfTime.contains(time)) return
        listOfTime.add(time)
        addChip(time)
    }

    private fun addChip(time: String, allowDelete: Boolean = true) {
        val chip = Chip(context)
        chip.text = time
        chip.id = ViewCompat.generateViewId()
        if (allowDelete) {
            chip.isCloseIconVisible = true
            chip.setOnCloseIconClickListener(this)
        } else {
            chip.isCloseIconVisible = false
        }
        chipGroup.addView(chip)
    }

    override fun onClick(view: View?) {
        if (view is Chip) {
            val time = view.text
            listOfTime.remove(time)
            chipGroup.removeView(view)
        }
    }

    private class StartStopTimePickerSaveState : BaseSavedState {

        var allowAdd = true

        constructor(source: Parcel) : super(source) {
            allowAdd = source.readByte() != 0.toByte()
        }

        constructor(superState: Parcelable?) : super(superState)

        override fun writeToParcel(parcel: Parcel, flags: Int) {
            super.writeToParcel(parcel, flags)
            parcel.writeByte(if (allowAdd) 1 else 0)
        }

        override fun describeContents(): Int {
            return 0
        }

        companion object CREATOR : Parcelable.Creator<StartStopTimePickerSaveState> {
            override fun createFromParcel(parcel: Parcel): StartStopTimePickerSaveState {
                return StartStopTimePickerSaveState(parcel)
            }

            override fun newArray(size: Int): Array<StartStopTimePickerSaveState?> {
                return arrayOfNulls(size)
            }
        }
    }
}
