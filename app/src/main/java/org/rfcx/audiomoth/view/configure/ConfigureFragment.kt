package org.rfcx.audiomoth.view.configure


import android.app.TimePickerDialog
import android.content.Context
import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.android.synthetic.main.alert_duration_layout.view.*
import kotlinx.android.synthetic.main.fragment_configure.*
import org.rfcx.audiomoth.R
import org.rfcx.audiomoth.entity.Stream
import org.rfcx.audiomoth.util.Firestore
import org.rfcx.audiomoth.util.getCalendar
import org.rfcx.audiomoth.util.getIntColor
import org.rfcx.audiomoth.util.toTimeString
import org.rfcx.audiomoth.view.CreateStreamActivity.Companion.DEVICES
import org.rfcx.audiomoth.view.CreateStreamActivity.Companion.DEVICE_ID
import java.util.*

class ConfigureFragment(stream: Stream) : Fragment(), OnItemClickListener {

    private val recordingPeriodAdapter by lazy { RecordingPeriodAdapter(this) }
    private lateinit var listener: ConfigureListener
    private val sampleRateList = arrayOf("8", "16", "32", "48", "96", "192", "256", "384")
    private val gainList = arrayOf("1 - Lowest", "2 - Low", "3 - Medium", "4 - High", "5 - Highest")
    private val duration = arrayOf(RECOMMENDED, CONTINUOUS, CUSTOM)

    private var gain = stream.gain
    private var sampleRate = stream.sampleRate
    private var sleepDuration = stream.sleepDuration
    private var recordingDuration = stream.recordingDuration

    private var startPeriod = getCalendar()
    private var endPeriod = getCalendar()
    private var recordingPeriod = stream.recordingPeriodList
    private var customRecordingPeriod = stream.customRecordingPeriod
    private var durationSelected = ""

    override fun onAttach(context: Context) {
        super.onAttach(context)
        listener = (context as ConfigureListener)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_configure, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setGainLayout()
        setNextOnClick()
        setSiteSpinner()
        setSampleRateLayout()
        setSleepDurationLayout()
        setCustomRecordingPeriod()
        setRecordingDurationLayout()
        setCustomRecordingPeriodRecyclerView()
        itemSelected(RECOMMENDED)
    }

    private fun setSiteSpinner() {
        val arrayAdapter =
            context?.let {
                ArrayAdapter(
                    it,
                    R.layout.support_simple_spinner_dropdown_item,
                    duration
                )
            }
        durationSpinner.adapter = arrayAdapter

        durationSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                durationSelected = duration[position]
                itemSelected(durationSelected)
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
            }
        }
    }

    fun itemSelected(item: String) {
        when (item) {
            RECOMMENDED -> context.let {
                if (it != null) {
                    setDuration(
                        false,
                        it.getIntColor(R.color.dark_gray),
                        it.getIntColor(R.color.dark_gray),
                        getString(R.string.second, "10"),
                        getString(R.string.second, "30")
                    )
                }
                sleepDuration = 10
                recordingDuration = 30
            }
            CONTINUOUS -> context.let {
                if (it != null) {
                    setDuration(
                        false,
                        it.getIntColor(R.color.dark_gray),
                        it.getIntColor(R.color.dark_gray),
                        getString(R.string.second, "0"),
                        getString(R.string.continuous)
                    )
                }
                sleepDuration = 0
                recordingDuration = 0
            }
            CUSTOM -> context.let {
                if (it != null) {
                    setDuration(
                        true,
                        it.getIntColor(R.color.text_black),
                        it.getIntColor(R.color.text_secondary),
                        getString(R.string.second, sleepDuration.toString()),
                        getString(R.string.second, recordingDuration.toString())
                    )
                }
            }
        }
    }

    private fun setDuration(
        enabled: Boolean,
        durationColor: Int,
        durationValueColor: Int,
        sleep: String,
        recording: String
    ) {
        sleepDurationLayout.isEnabled = enabled
        recordingDurationLayout.isEnabled = enabled
        sleepDurationTextView.setTextColor(durationColor)
        sleepDurationValueTextView.text = sleep
        sleepDurationValueTextView.setTextColor(durationValueColor)
        recordingDurationTextView.setTextColor(durationColor)
        recordingDurationValueTextView.text = recording
        recordingDurationValueTextView.setTextColor(durationValueColor)
    }

    private fun setCustomRecordingPeriodRecyclerView() {
        customRecordingPeriodRecyclerView.apply {
            val alertsLayoutManager = LinearLayoutManager(context)
            layoutManager = alertsLayoutManager
            adapter = recordingPeriodAdapter
        }
        recordingPeriodAdapter.items = recordingPeriod
    }

    private fun setCustomRecordingPeriod() {
        addRecordingPeriodTextView.text = getString(R.string.add_recording_period).toUpperCase()
        startPeriodTextView.text = startPeriod.toTimeString()
        endPeriodTextView.text = endPeriod.toTimeString()
        customRecordingPeriodSwitch.isChecked = customRecordingPeriod
        isChecked(customRecordingPeriod)

        startPeriodLayout.setOnClickListener {
            setTimePickerDialog(startPeriodTextView, startPeriod, true)
        }

        endPeriodLayout.setOnClickListener {
            setTimePickerDialog(endPeriodTextView, endPeriod, false)
        }

        customRecordingPeriodSwitch.setOnCheckedChangeListener { _, isChecked ->
            customRecordingPeriod = isChecked
            isChecked(isChecked)
        }

        addRecordingPeriodTextView.setOnClickListener {
            recordingPeriod.add(
                "${startPeriod.toTimeString()} - ${endPeriod.toTimeString()}"
            )
            recordingPeriodAdapter.items = recordingPeriod
        }
    }

    private fun isChecked(isChecked: Boolean) {
        if (isChecked) {
            addRecordingPeriodGroupView.visibility = View.VISIBLE
            alwaysRecordingTextView.visibility = View.GONE
        } else {
            addRecordingPeriodGroupView.visibility = View.GONE
            alwaysRecordingTextView.visibility = View.VISIBLE
        }
    }

    private fun setNextOnClick() {
        nextButton.setOnClickListener {
            if (arguments?.containsKey(DEVICE_ID) == true && arguments?.containsKey(
                    ConfigureActivity.STREAM_NAME
                ) == true
            ) {
                arguments?.let {
                    val deviceId = it.getString(DEVICE_ID)
                    val streamName = it.getString(ConfigureActivity.STREAM_NAME)
                    if (deviceId != null && streamName != null) {
                        updateStream(deviceId, streamName)
                        listener.openSync()
                    }
                }
            }
        }
    }

    private fun updateStream(deviceId: String, streamName: String) {
        val docRef = Firestore().db.collection(DEVICES).document(deviceId)
        docRef.collection("streams").document(streamName)
            .update(
                mapOf(
                    "sampleRateKiloHertz" to sampleRate,
                    "gain" to gain,
                    "sleepDurationSecond" to sleepDuration,
                    "recordingDurationSecond" to recordingDuration,
                    "customRecordingPeriod" to customRecordingPeriod,
                    "recordingPeriodList" to recordingPeriod
                )
            )
    }

    private fun setSampleRateLayout() {
        sampleRateValueTextView.text = getString(R.string.kilohertz, sampleRate.toString())
        sampleRateLayout.setOnClickListener {
            val builder = context?.let { it1 -> AlertDialog.Builder(it1) }
            if (builder != null) {
                builder.setTitle(R.string.choose_sample_rate)
                    ?.setItems(sampleRateList) { dialog, i ->
                        try {
                            sampleRateValueTextView.text =
                                getString(R.string.kilohertz, sampleRateList[i])
                            sampleRate = sampleRateList[i].toInt()
                        } catch (e: IllegalArgumentException) {
                            dialog.dismiss()
                        }
                    }
                val dialog = builder.create()
                dialog?.show()
            }
        }
    }

    private fun setGainLayout() {
        gainValueTextView.text = gainList[gain - 1]

        gainLayout.setOnClickListener {
            val builder = context?.let { it1 -> AlertDialog.Builder(it1) }
            if (builder != null) {
                builder.setTitle(R.string.choose_gain)?.setItems(gainList) { dialog, i ->
                    try {
                        gainValueTextView.text = gainList[i]
                        gain = i + 1
                    } catch (e: IllegalArgumentException) {
                        dialog.dismiss()
                    }
                }
                val dialog = builder.create()
                dialog?.show()
            }
        }
    }

    private fun setSleepDurationLayout() {
        sleepDurationLayout.setOnClickListener {
            setAlertDialog(
                getString(R.string.enter_sleep_duration),
                getString(R.string.sleep_duration),
                sleepDurationValueTextView
            )
        }
    }

    private fun setRecordingDurationLayout() {
        recordingDurationLayout.setOnClickListener {
            setAlertDialog(
                getString(R.string.enter_recording_duration),
                getString(R.string.recording_duration),
                recordingDurationValueTextView
            )
        }
    }

    private fun setAlertDialog(title: String, hint: String, textView: TextView) {
        val view = layoutInflater.inflate(R.layout.alert_duration_layout, null)
        view.durationTextInput.hint = hint
        val builder = context?.let { it1 -> AlertDialog.Builder(it1) }
        if (builder != null) {
            builder.setTitle(title)
            builder.setView(view)

            builder.setPositiveButton(getString(R.string.ok)) { dialog, _ ->
                val duration = view.durationEditText.text.toString().trim()
                if (duration.isNotEmpty()) {
                    textView.text = getString(R.string.second, duration)
                    if (hint == getString(R.string.sleep_duration)) {
                        sleepDuration = duration.toInt()
                    } else {
                        recordingDuration = duration.toInt()
                    }
                }
                dialog.dismiss()
            }

            builder.setNegativeButton(getString(R.string.cancel)) { dialog, _ ->
                dialog.dismiss()
            }

            val alertDialog = builder.create()
            alertDialog.show()

            val buttonNeutral = alertDialog.getButton(DialogInterface.BUTTON_NEGATIVE)
            context?.let { ContextCompat.getColor(it, R.color.text_secondary) }?.let {
                buttonNeutral.setTextColor(
                    it
                )
            }
        }
    }

    private fun setTimePickerDialog(
        textView: TextView,
        calendarBefore: Calendar,
        isStartPeriod: Boolean
    ) {
        val timeSetListener = TimePickerDialog.OnTimeSetListener { _, hour, minute ->
            calendarBefore.set(Calendar.HOUR_OF_DAY, hour)
            calendarBefore.set(Calendar.MINUTE, minute)
            textView.text = calendarBefore.toTimeString()
            if (isStartPeriod) {
                startPeriod = calendarBefore
            } else {
                endPeriod = calendarBefore
            }
        }
        val timePickerDialog = TimePickerDialog(
            context,
            timeSetListener,
            calendarBefore.get(Calendar.HOUR_OF_DAY),
            calendarBefore.get(Calendar.MINUTE),
            true
        )
        timePickerDialog.show()

        val buttonNeutral = timePickerDialog.getButton(DialogInterface.BUTTON_NEGATIVE)
        context?.let { ContextCompat.getColor(it, R.color.text_secondary) }?.let {
            buttonNeutral.setTextColor(
                it
            )
        }
    }

    override fun onItemClick(position: Int) {
        recordingPeriod.removeAt(position)
        recordingPeriodAdapter.items = recordingPeriod
    }

    companion object {
        const val RECOMMENDED = "Recommended"
        const val CONTINUOUS = "Continuous duration"
        const val CUSTOM = "Custom duration"

        fun newInstance(deviceId: String, streamName: String, streams: Stream): ConfigureFragment {
            return ConfigureFragment(streams).apply {
                arguments = Bundle().apply {
                    putString(DEVICE_ID, deviceId)
                    putString(ConfigureActivity.STREAM_NAME, streamName)
                }
            }
        }
    }
}

interface OnItemClickListener {
    fun onItemClick(position: Int)
}
