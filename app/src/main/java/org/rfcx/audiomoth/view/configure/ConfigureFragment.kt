package org.rfcx.audiomoth.view.configure


import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Context.ALARM_SERVICE
import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.android.synthetic.main.fragment_configure.*
import org.rfcx.audiomoth.R
import org.rfcx.audiomoth.entity.Profile
import org.rfcx.audiomoth.util.NotificationBroadcastReceiver
import org.rfcx.audiomoth.view.CreateStreamActivity.Companion.DEVICE_ID
import org.rfcx.audiomoth.view.DeploymentProtocol
import org.rfcx.audiomoth.view.UserListener
import java.util.*
import kotlin.collections.ArrayList

class ConfigureFragment : Fragment(), OnItemClickListener {

    private var deploymentProtocol: DeploymentProtocol? = null
    private var userListener: UserListener? = null
    private val recordingPeriodAdapter by lazy { RecordingPeriodAdapter(this) }
    private val timeAdapter by lazy { TimeAdapter(this, context) }

    private val sampleRateList = arrayOf("8", "16", "32", "48", "96", "192", "256", "384")
    private val gainList = arrayOf("1 - Lowest", "2 - Low", "3 - Medium", "4 - High", "5 - Highest")
    private var timeList = arrayListOf(
        "00:00",
        "01:00",
        "02:00",
        "03:00",
        "04:00",
        "05:00",
        "06:00",
        "07:00",
        "08:00",
        "09:00",
        "10:00",
        "11:00",
        "12:00",
        "13:00",
        "14:00",
        "15:00",
        "16:00",
        "17:00",
        "18:00",
        "19:00",
        "20:00",
        "21:00",
        "22:00",
        "23:00"
    )

    private var gain = 0
    private var sampleRate = 8      // default sampleRate is 8
    private var sleepDuration = 0
    private var recordingDuration = 0
    private var recordingPeriod = ArrayList<String>()
    private var customRecordingPeriod = recordingPeriod.isNotEmpty()
    private var durationSelected = RECOMMENDED
    private var profile: Profile? = null
    private var timeState = ArrayList<TimeItem>()

    override fun onAttach(context: Context) {
        super.onAttach(context)
        deploymentProtocol = context as DeploymentProtocol
        userListener = context as UserListener
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

        profile = userListener?.getProfile()
        deploymentProtocol?.hideCompleteButton()

        for (time in timeList) {
            profile?.let {
                timeState.add(TimeItem(time, it.recordingPeriodList.contains(time)))
            }
        }

        if (profile != null) {
            profile?.let {
                durationSelectedItem(it.durationSelected)
            }
        } else {
            durationSelectedItem(durationSelected)
        }

        profile?.let {
            sleepDurationEditText.setText(it.sleepDuration.toString())
            recordingDurationEditText.setText(it.recordingDuration.toString())
        }

        setGainLayout()
        setSampleRateLayout()
        setTimeRecyclerView()
        setCustomRecordingPeriod()
        createNotificationChannel()
        setNextOnClick()
        setRadioGroup()
    }

    private fun setRadioGroup() {
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

    private fun setTimeRecyclerView() {
        timeRecyclerView.apply {
            val timeLayoutManager =
                LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
            layoutManager = timeLayoutManager
            adapter = timeAdapter
        }
        timeAdapter.items = timeState
    }

    private fun setCustomRecordingPeriod() {
        profile?.let {
            val checker = it.recordingPeriodList.isNotEmpty()
            isChecked(checker)
            customRecordingPeriodSwitch.isChecked = checker
            customRecordingPeriodSwitch.setOnCheckedChangeListener { _, isChecked ->
                customRecordingPeriod = isChecked
                isChecked(isChecked)
            }
        }
    }

    private fun isChecked(isChecked: Boolean) {
        if (isChecked) {
            timeRecyclerView.visibility = View.VISIBLE
            alwaysRecordingTextView.visibility = View.GONE
        } else {
            timeRecyclerView.visibility = View.GONE
            alwaysRecordingTextView.visibility = View.VISIBLE
        }
    }

    private fun setNextOnClick() {
        nextButton.setOnClickListener {
            deploymentProtocol?.nextStep()

            if (arguments?.containsKey(DEVICE_ID) == true) {
                arguments?.let {
                    val deviceId = it.getString(DEVICE_ID)
                    if (deviceId != null) {
                        val timeRecordingPeriod = arrayListOf<String>()
                        if (customRecordingPeriod) {
                            timeState.forEach { timeStatus ->
                                if (timeStatus.state) {
                                    timeRecordingPeriod.add(timeStatus.time)
                                }
                            }
                            recordingPeriod = timeRecordingPeriod
                        } else {
                            recordingPeriod = arrayListOf()
                        }

                        when (durationSelected) {
                            RECOMMENDED -> {
                                recordingDuration = 5
                                sleepDuration = 10
                            }

                            CONTINUOUS -> {
                                recordingDuration = 0
                                sleepDuration = 0
                            }

                            CUSTOM -> {
                                recordingDuration =
                                    recordingDurationEditText.text.toString().toInt()
                                sleepDuration = sleepDurationEditText.text.toString().toInt()
                            }
                        }
                        // Todo: start notification here
                    }
                }
            }
        }
    }

    private fun createNotificationChannel() {
        val notificationManager =
            context?.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationChannel =
                NotificationChannel(CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_HIGH)
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

        val calendar =
            Calendar.getInstance()  // Todo: Change to the date of the battery will run out.
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MINUTE, 30)
        calendar.set(Calendar.HOUR, 9)  // receives a notification at 9:30
        calendar.get(Calendar.MONTH)
        calendar.get(Calendar.DAY_OF_MONTH)
        calendar.get(Calendar.YEAR)
        // Todo: Remove comment to receives a notification 3 days before battery is due to run out
        // calendar.add(Calendar.DATE, -3)

        alarmManager.setExact(
            AlarmManager.RTC_WAKEUP,
            calendar.timeInMillis,
            pendingIntent
        )
    }

    private fun setSampleRateLayout() {
        if (profile != null) {
            profile?.let {
                sampleRateValueTextView.text =
                    getString(R.string.kilohertz, it.sampleRate.toString())
                sampleRate = it.sampleRate
            }
        } else {
            sampleRateValueTextView.text = getString(R.string.kilohertz, sampleRate.toString())
        }

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
                dialog.show()
            }
        }
    }

    private fun setGainLayout() {
        if (profile != null) {
            profile?.let {
                gainValueTextView.text = gainList[it.gain - 1]
                gain = it.gain
            }
        } else {
            gainValueTextView.text = gainList[0]
        }

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
                dialog.show()
            }
        }
    }

    override fun onItemClick(position: Int) {
        recordingPeriod.removeAt(position)
        recordingPeriodAdapter.items = recordingPeriod
    }

    override fun onTimeItemClick(item: TimeItem, position: Int) {
        timeState[position] = TimeItem(item.time, !item.state)
        timeAdapter.items = timeState
    }

    companion object {
        const val RECOMMENDED = "RECOMMENDED"
        const val CONTINUOUS = "CONTINUOUS"
        const val CUSTOM = "CUSTOM"
        const val CHANNEL_ID = "AudioMoth Notification"
        const val CHANNEL_NAME = "Notification"

        fun newInstance(): ConfigureFragment {
            return ConfigureFragment()
        }
    }
}

interface OnItemClickListener {
    fun onItemClick(position: Int)
    fun onTimeItemClick(item: TimeItem, position: Int)
}

data class TimeItem(val time: String, val state: Boolean)
