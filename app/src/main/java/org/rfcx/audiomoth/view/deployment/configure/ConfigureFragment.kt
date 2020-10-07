package org.rfcx.audiomoth.view.deployment.configure

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.graphics.Color
import android.graphics.Rect
import android.os.Build
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.android.synthetic.main.fragment_configure.*
import org.rfcx.audiomoth.R
import org.rfcx.audiomoth.entity.Profile
import org.rfcx.audiomoth.util.EdgeConfigure
import org.rfcx.audiomoth.util.convertToStopStartPeriods
import org.rfcx.audiomoth.view.deployment.EdgeDeploymentProtocol

class ConfigureFragment : Fragment(), OnItemClickListener {

    private var edgeDeploymentProtocol: EdgeDeploymentProtocol? = null
    private val timeAdapter by lazy { TimeAdapter(this, context) }
    private val gainList by lazy { resources.getStringArray(R.array.edge_gains) }
    private val sampleRateList = EdgeConfigure.configureSampleRate
    private val timeList = EdgeConfigure.configureTimes
    private var gain = EdgeConfigure.GAIN_DEFAULT
    private var sampleRate = EdgeConfigure.SAMPLE_RATE_DEFAULT
    private var sleepDuration = EdgeConfigure.RECORDING_DURATION_DEFAULT
    private var recordingDuration = EdgeConfigure.SLEEP_DURATION_DEFAULT
    private var recordingPeriod = ArrayList<String>()
    private var customRecordingPeriod = recordingPeriod.isNotEmpty()
    private var durationSelected = EdgeConfigure.DURATION_SELECTED_DEFAULT
    private var profile: Profile? = null
    private var timeState = ArrayList<TimeItem>()

    override fun onAttach(context: Context) {
        super.onAttach(context)
        edgeDeploymentProtocol = context as EdgeDeploymentProtocol
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

        edgeDeploymentProtocol?.let {
            it.showToolbar()
            it.setToolbarTitle()
        }

        view.viewTreeObserver.addOnGlobalLayoutListener { setOnFocusEditText() }

        profile = edgeDeploymentProtocol?.getProfile()

        if (profile?.name != null) {
            profileEditText.setText(profile?.name)
        }

        profile?.durationSelected.let {
            if (it != null) {
                durationSelected = it
            }
        }

        for (time in timeList) {
            profile?.let {
                timeState.add(
                    TimeItem(
                        time,
                        it.recordingPeriodList.contains(time)
                    )
                )
            }
        }

        if (profile != null) {
            durationSelectedItem(profile!!.durationSelected)
        } else {
            durationSelectedItem(durationSelected)
        }

        profile?.let {
            sleepDurationEditText.setText(it.sleepDuration.toString())
            recordingDurationEditText.setText(it.recordingDuration.toString())
        }

        setNextButton(true)
        setGainLayout()
        setSampleRateLayout()
        setTimeRecyclerView()
        setCustomRecordingPeriod()
        createNotificationChannel()
        setNextOnClick()
        setRadioGroup()
        checkMinimumOfDuration()
        setHideKeyboard()
    }

    private fun setOnFocusEditText() {
        val screenHeight: Int = view?.rootView?.height ?: 0
        val r = Rect()
        view?.getWindowVisibleDisplayFrame(r)
        val keypadHeight: Int = screenHeight - r.bottom
        if (keypadHeight > screenHeight * 0.15) {
            nextButton.visibility = View.GONE
        } else {
            if (nextButton != null) {
                nextButton.visibility = View.VISIBLE
            }
        }
    }

    private fun setHideKeyboard() {
        recordingDurationEditText.setOnEditorActionListener { v, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                recordingDurationEditText.clearFocus()
                recordingDurationEditText.hideKeyboard()
            }
            false
        }

        sleepDurationEditText.setOnEditorActionListener { v, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                sleepDurationEditText.clearFocus()
                sleepDurationEditText.hideKeyboard()
            }
            false
        }

        profileEditText.setOnEditorActionListener { v, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                profileEditText.clearFocus()
                profileEditText.hideKeyboard()
            }
            false
        }
    }

    private fun checkMinimumOfDuration() {
        recordingDurationEditText.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(p0: Editable?) {
                if (p0 != null && p0.toString() != "") {
                    if (p0.toString().toInt() < MINIMUM_RECORDING_DURATION) {
                        recordingDurationEditText.error = getString(R.string.minimum_1_second)
                    }
                }
            }

            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}

            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}
        })

        sleepDurationEditText.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(p0: Editable?) {
                if (p0 != null && p0.toString() != "") {
                    if (p0.toString().toInt() < MINIMUM_SLEEP_DURATION) {
                        sleepDurationEditText.error = getString(R.string.minimum_5_second)
                    }
                }
            }

            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}

            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}
        })
    }

    private fun setRadioGroup() {
        radioGroup.setOnCheckedChangeListener { _, checkedId ->
            when (checkedId) {
                R.id.recommendedRadioButton -> {
                    durationSelected =
                        RECOMMENDED
                    durationSelectedItem(RECOMMENDED)
                }
                R.id.continuousRadioButton -> {
                    durationSelected =
                        CONTINUOUS
                    durationSelectedItem(CONTINUOUS)
                }
                R.id.customRadioButton -> {
                    durationSelected =
                        CUSTOM
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
            alwaysRecordingTextView.visibility = View.INVISIBLE
        } else {
            timeRecyclerView.visibility = View.GONE
            alwaysRecordingTextView.visibility = View.VISIBLE
        }
    }

    private fun setupData() {
        val array = arrayListOf<Boolean>()
        timeState.forEach { timeStatus ->
            array.add(timeStatus.state)
        }

        when (durationSelected) {
            RECOMMENDED -> {
                recordingDuration = 300
                sleepDuration = 600
            }

            CONTINUOUS -> {
                recordingDuration = 60
                sleepDuration = 5
            }

            CUSTOM -> {
                recordingDuration =
                    recordingDurationEditText.text.toString().toInt()
                sleepDuration = sleepDurationEditText.text.toString().toInt()
            }
        }

        val count = convertToStopStartPeriods(array.toTypedArray())?.size
        if (count != null && count > 4) {
            setNextButton(true)
            Toast.makeText(context, R.string.maximum_ranges, Toast.LENGTH_LONG).show()
        } else {
            timeState.forEach { timeStatus ->
                if (timeStatus.state) {
                    recordingPeriod.add(timeStatus.time)
                }
            }
            updateProfile()
        }
    }

    private fun setNextButton(show: Boolean) {
        nextButton.visibility = if (show) View.VISIBLE else View.GONE
    }

    private fun setNextOnClick() {
        nextButton.setOnClickListener {
            setNextButton(false)
            setupData()
        }
    }

    private fun updateProfile() {
        val profileName = profileEditText.text
        val profileTemp = Profile(
            gain = gain,
            name = profileName?.trim()?.toString() ?: "",
            sampleRate = sampleRate,
            recordingDuration = recordingDuration,
            sleepDuration = sleepDuration,
            recordingPeriodList = recordingPeriod,
            durationSelected = durationSelected
        )

        // is new Profile?
        val newProfile = profile?.let {
            if (it.name == profileTemp.name) {
                profileTemp.id = it.id
            }
            profileTemp
        } ?: kotlin.run {
            profileTemp
        }

        edgeDeploymentProtocol?.setDeploymentConfigure(newProfile)
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
            val builder = context?.let { it1 -> AlertDialog.Builder(it1, R.style.DialogCustom) }
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
                gainValueTextView.text = gainList[it.gain]
                gain = it.gain
            }
        } else {
            gainValueTextView.text = gainList[0]
        }

        gainLayout.setOnClickListener {
            val builder = context?.let { it1 -> AlertDialog.Builder(it1, R.style.DialogCustom) }
            if (builder != null) {
                builder.setTitle(R.string.choose_gain)?.setItems(gainList) { dialog, i ->
                    try {
                        gainValueTextView.text = gainList[i]
                        gain = i
                    } catch (e: IllegalArgumentException) {
                        dialog.dismiss()
                    }
                }
                val dialog = builder.create()
                dialog.show()
            }
        }
    }

    override fun onTimeItemClick(item: TimeItem, position: Int) {
        timeState[position] = TimeItem(item.time, !item.state)
        timeAdapter.items = timeState
    }

    private fun View.hideKeyboard() = this.let {
        val inputManager =
            context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        inputManager.hideSoftInputFromWindow(windowToken, 0)
    }

    companion object {
        const val RECOMMENDED = "RECOMMENDED"
        const val CONTINUOUS = "CONTINUOUS"
        const val CUSTOM = "CUSTOM"
        const val CHANNEL_ID = "AudioMoth Notification"
        const val CHANNEL_NAME = "Notification"
        const val MINIMUM_RECORDING_DURATION = 1
        const val MINIMUM_SLEEP_DURATION = 5

        fun newInstance(): ConfigureFragment {
            return ConfigureFragment()
        }
    }
}

interface OnItemClickListener {
    fun onTimeItemClick(item: TimeItem, position: Int)
}

data class TimeItem(val time: String, val state: Boolean)
