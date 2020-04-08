package org.rfcx.audiomoth.view.configure


import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.android.synthetic.main.fragment_configure.*
import org.rfcx.audiomoth.R
import org.rfcx.audiomoth.entity.Stream
import org.rfcx.audiomoth.util.Firestore
import org.rfcx.audiomoth.view.CreateStreamActivity.Companion.DEVICES
import org.rfcx.audiomoth.view.CreateStreamActivity.Companion.DEVICE_ID
import org.rfcx.audiomoth.view.configure.ConfigureActivity.Companion.FROM

class ConfigureFragment(stream: Stream) : Fragment(), OnItemClickListener {

    private val recordingPeriodAdapter by lazy { RecordingPeriodAdapter(this) }
    private val timeAdapter by lazy { TimeAdapter(this, context) }
    private lateinit var listener: ConfigureListener
    private val sampleRateList = arrayOf("8", "16", "32", "48", "96", "192", "256", "384")
    private val gainList = arrayOf("1 - Lowest", "2 - Low", "3 - Medium", "4 - High", "5 - Highest")

    private var gain = stream.gain
    private var sampleRate = stream.sampleRate
    private var sleepDuration = stream.sleepDuration
    private var recordingDuration = stream.recordingDuration
    private var recordingPeriod = stream.recordingPeriodList
    private var customRecordingPeriod = stream.customRecordingPeriod
    private var durationSelected = stream.durationSelected

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

    private val timeState = mutableMapOf<String, Boolean>()

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
        setTimeRecyclerView()
        setCustomRecordingPeriod()
        durationSelectedItem(durationSelected)

        for (time in timeList) {
            timeState[time] = recordingPeriod.contains(time)
        }

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
        isChecked(customRecordingPeriod)
        customRecordingPeriodSwitch.isChecked = customRecordingPeriod
        customRecordingPeriodSwitch.setOnCheckedChangeListener { _, isChecked ->
            customRecordingPeriod = isChecked
            isChecked(isChecked)
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
                        val timeRecordingPeriod = arrayListOf<String>()
                        if (customRecordingPeriod) {
                            timeState.forEach { timeStatus ->
                                if (timeStatus.value) {
                                    timeRecordingPeriod.add(timeStatus.key)
                                }
                            }
                            recordingPeriod = timeRecordingPeriod
                        }

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
                    "recordingPeriodList" to recordingPeriod,
                    "durationSelected" to durationSelected
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

    override fun onItemClick(position: Int) {
        recordingPeriod.removeAt(position)
        recordingPeriodAdapter.items = recordingPeriod
    }

    override fun onTimeItemClick(time: String) {
        val status = timeState[time]
        if (status != null) {
            timeState[time] = !status
        }
        timeAdapter.items = timeState
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
    fun onTimeItemClick(time: String)
}
