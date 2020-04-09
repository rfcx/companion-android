package org.rfcx.audiomoth.view.configure


import android.app.*
import android.content.Context
import android.content.Context.ALARM_SERVICE
import android.content.DialogInterface
import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.android.synthetic.main.fragment_configure.*
import org.rfcx.audiomoth.R
import org.rfcx.audiomoth.entity.Stream
import org.rfcx.audiomoth.util.Firestore
import org.rfcx.audiomoth.util.NotificationBroadcastReceiver
import org.rfcx.audiomoth.util.getCalendar
import org.rfcx.audiomoth.util.toTimeString
import org.rfcx.audiomoth.view.CreateStreamActivity.Companion.DEVICES
import org.rfcx.audiomoth.view.CreateStreamActivity.Companion.DEVICE_ID
import org.rfcx.audiomoth.view.configure.ConfigureActivity.Companion.FROM
import java.util.*

class ConfigureFragment(stream: Stream) : Fragment(), OnItemClickListener {

    private val recordingPeriodAdapter by lazy { RecordingPeriodAdapter(this) }
    private lateinit var listener: ConfigureListener
    private val sampleRateList = arrayOf("8", "16", "32", "48", "96", "192", "256", "384")
    private val gainList = arrayOf("1 - Lowest", "2 - Low", "3 - Medium", "4 - High", "5 - Highest")

    private var gain = stream.gain
    private var sampleRate = stream.sampleRate
    private var sleepDuration = stream.sleepDuration
    private var recordingDuration = stream.recordingDuration

    private var startPeriod = getCalendar()
    private var endPeriod = getCalendar()
    private var recordingPeriod = stream.recordingPeriodList
    private var customRecordingPeriod = stream.customRecordingPeriod
    private var durationSelected = stream.durationSelected

    private val channelId = "org.rfcx.audiomoth.view.configure"
    private val name = "Notification"

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
        setSampleRateLayout()
        setCustomRecordingPeriod()
        createNotificationChannel()
        setCustomRecordingPeriodRecyclerView()
        durationSelectedItem(durationSelected)

        if (arguments?.containsKey(FROM) == true) {
            arguments?.let {
                val from = it.getString(FROM)
                if (from != null) {
                    if (from == DASHBOARD_STREAM) {
                        sleepDurationEditText.setText(sleepDuration.toString())
                        recordingDurationEditText.setText(recordingDuration.toString())
                    }
                }
            }
        }

        radioGroup.setOnCheckedChangeListener { _, checkedId ->
            when (checkedId) {
                R.id.recommendedRadioButton -> {
                    durationSelected = RECOMMENDED
                    durationSelectedItem(RECOMMENDED)
                }
                R.id.continuousRadioButton -> {
                    durationSelected = CONTINUOUS
                    durationSelectedItem(CONTINUOUS)
                }
                R.id.customRadioButton -> {
                    durationSelected = CUSTOM
                    durationSelectedItem(CUSTOM)
                }
            }
        }
    }

    private fun durationSelectedItem(selected: String) {
        when (selected) {
            RECOMMENDED -> {
                recommendedRadioButton.isChecked = true
                setDuration(
                    recommendedText = true,
                    continuousText = false,
                    durationTextInput = false
                )
            }

            CONTINUOUS -> {
                continuousRadioButton.isChecked = true
                setDuration(
                    recommendedText = false,
                    continuousText = true,
                    durationTextInput = false
                )
            }

            CUSTOM -> {
                customRadioButton.isChecked = true
                setDuration(
                    recommendedText = false,
                    continuousText = false,
                    durationTextInput = true
                )
            }
        }
    }

    private fun setDuration(
        recommendedText: Boolean,
        continuousText: Boolean,
        durationTextInput: Boolean
    ) {
        recommendedTextView.visibility = if (recommendedText) View.VISIBLE else View.GONE
        continuousTextView.visibility = if (continuousText) View.VISIBLE else View.GONE
        recordingDurationTextInput.visibility = if (durationTextInput) View.VISIBLE else View.GONE
        sleepDurationTextInput.visibility = if (durationTextInput) View.VISIBLE else View.GONE
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
            if (durationSelected == CUSTOM) {
                recordingDuration = recordingDurationEditText.text.toString().toInt()
                sleepDuration = sleepDurationEditText.text.toString().toInt()
            }
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
                        notification()
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
                    "recordingPeriodList" to recordingPeriod,
                    "durationSelected" to durationSelected
                )
            )
    }

    private fun createNotificationChannel() {
        val notificationManager =
            context?.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationChannel =
                NotificationChannel(channelId, name, NotificationManager.IMPORTANCE_HIGH)
            notificationChannel.enableLights(true)
            notificationChannel.lightColor = Color.GREEN
            notificationChannel.enableVibration(false)
            notificationManager.createNotificationChannel(notificationChannel)
        }
    }

    private fun notification() {
        // todo: pass deviceId to DashboardStreamActivity
        val intent = Intent(context, NotificationBroadcastReceiver::class.java)
        val pendingIntent =
            PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)

        val alarmManager = context?.getSystemService(ALARM_SERVICE) as AlarmManager

        alarmManager.set(AlarmManager.RTC, System.currentTimeMillis() + 1000 * 10, pendingIntent)
        Toast.makeText(context, "Alarm is set", Toast.LENGTH_SHORT).show()

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
        const val CONTINUOUS = "Continuous"
        const val CUSTOM = "Custom"
        const val CREATE_STREAM = "CREATE_STREAM"
        const val DASHBOARD_STREAM = "DASHBOARD_STREAM"

        fun newInstance(
            deviceId: String,
            streamName: String,
            streams: Stream,
            from: String
        ): ConfigureFragment {
            return ConfigureFragment(streams).apply {
                arguments = Bundle().apply {
                    putString(DEVICE_ID, deviceId)
                    putString(FROM, from)
                    putString(ConfigureActivity.STREAM_NAME, streamName)
                }
            }
        }
    }
}

interface OnItemClickListener {
    fun onItemClick(position: Int)
}
