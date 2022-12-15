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
import org.rfcx.companion.entity.time.Time
import org.rfcx.companion.entity.time.TimeRange
import org.rfcx.companion.util.time.TimeRangeUtils
import org.rfcx.companion.util.time.toListTimeRange
import org.rfcx.companion.util.time.toTimeRange

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

    private var tempStartTime: Time = Time()
    private var tempStopTime: Time = Time()
    var listOfTime = arrayListOf<TimeRange>()

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
            .setPositiveButtonText(R.string.next)
            .setNegativeButtonText(R.string.back)
            .build()

        val stopPicker = MaterialTimePicker.Builder()
            .setTimeFormat(TimeFormat.CLOCK_24H)
            .setHour(0)
            .setMinute(0)
            .setTitleText(stopTitle)
            .setPositiveButtonText(R.string.next)
            .setNegativeButtonText(R.string.back)
            .build()

        startPicker.addOnPositiveButtonClickListener {
            val time = Time(startPicker.hour, startPicker.minute)
            tempStartTime = time
            if (fragmentManager == null) return@addOnPositiveButtonClickListener
            stopPicker.show(fragmentManager!!, "StopTimePicker")
        }

        stopPicker.addOnPositiveButtonClickListener {
            val time = Time(stopPicker.hour, stopPicker.minute)
            tempStopTime = time
            addTimeOff(TimeRange(tempStartTime, tempStopTime))
        }

        addChip.setOnClickListener {
            if (fragmentManager == null) return@setOnClickListener
            startPicker.show(fragmentManager!!, "StartTimePicker")
        }
    }

    fun setTimes(times: String?, toOpposite: Boolean = false) {
        if (times == null) return
        listOfTime.clear()
        if (toOpposite) {
            listOfTime.addAll(TimeRangeUtils.toOppositeTimes(times.toListTimeRange()))
        } else {
            listOfTime.addAll(times.toListTimeRange())
        }
        setChip(listOfTime, allowAdd)
    }

    private fun clearAllChips() {
        chipGroup.removeViews(1, chipGroup.childCount - 1)
    }

    private fun setChip(times: List<TimeRange>, isAddAllowed: Boolean) {
        clearAllChips()
        times.forEach {
            addChip(it.toStringFormat(), isAddAllowed)
        }
    }

    private fun addTimeOff(timeRange: TimeRange) {
        if (listOfTime.contains(timeRange)) return
        listOfTime.add(timeRange)
        listOfTime = ArrayList(TimeRangeUtils.simplifyTimes(listOfTime).map { it.copy() })
        setChip(listOfTime, true)
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
            listOfTime.remove(time.toString().toTimeRange())
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
