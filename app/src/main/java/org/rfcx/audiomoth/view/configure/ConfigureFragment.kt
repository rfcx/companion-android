package org.rfcx.audiomoth.view.configure


import android.annotation.SuppressLint
import android.app.TimePickerDialog
import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import kotlinx.android.synthetic.main.alert_duration_layout.view.*
import kotlinx.android.synthetic.main.fragment_configure.*
import org.rfcx.audiomoth.R
import org.rfcx.audiomoth.util.Firestore
import org.rfcx.audiomoth.view.CreateStreamActivity.Companion.DEVICES
import org.rfcx.audiomoth.view.CreateStreamActivity.Companion.DEVICE_ID
import java.text.SimpleDateFormat
import java.util.*

class ConfigureFragment : Fragment() {

    private val sampleRateList = arrayOf("8", "16", "32", "48", "96", "192", "256", "384")
    private val gainList = arrayOf("1 - Lowest", "2 - Low", "3 - Medium", "4 - High", "5 - Highest")
    val calendar = Calendar.getInstance()

    var gain = 0
    var sampleRate = 0
    var sleepDuration = 0
    var recordingDuration = 0

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_configure, container, false)
    }

    @SuppressLint("SimpleDateFormat")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        addRecordingPeriodTextView.text = getString(R.string.add_recording_period).toUpperCase()

        setSampleRateLayout()
        setGainLayout()
        setSleepDurationLayout()
        setRecordingDurationLayout()

        startPeriodTextView.text = SimpleDateFormat("HH:mm").format(calendar.time)
        endPeriodTextView.text = SimpleDateFormat("HH:mm").format(calendar.time)

        startPeriodLayout.setOnClickListener {
            setStartPeriod(startPeriodTextView)
        }

        endPeriodLayout.setOnClickListener {
            setStartPeriod(endPeriodTextView)
        }

        customRecordingPeriodSwitch.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                addRecordingPeriodGroupView.visibility = View.VISIBLE
                alwaysRecordingTextView.visibility = View.GONE
            } else {
                addRecordingPeriodGroupView.visibility = View.GONE
                alwaysRecordingTextView.visibility = View.VISIBLE
            }
        }

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
                    "recordingDurationSecond" to recordingDuration
                )
            )
    }

    private fun setSampleRateLayout() {
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

            builder.setNeutralButton(getString(R.string.cancel)) { dialog, _ ->
                dialog.dismiss()
            }

            val alertDialog = builder.create()
            alertDialog.show()

            val buttonNeutral = alertDialog.getButton(DialogInterface.BUTTON_NEUTRAL)
            context?.let { it1 -> ContextCompat.getColor(it1, R.color.text_secondary) }
                ?.let { it2 ->
                    buttonNeutral.setTextColor(
                        it2
                    )
                }
        }
    }

    @SuppressLint("SimpleDateFormat")
    private fun setStartPeriod(textView: TextView) {
        val cal = Calendar.getInstance()
        val timeSetListener = TimePickerDialog.OnTimeSetListener { _, hour, minute ->
            cal.set(Calendar.HOUR_OF_DAY, hour)
            cal.set(Calendar.MINUTE, minute)
            textView.text = SimpleDateFormat("HH:mm").format(cal.time)
        }
        TimePickerDialog(
            context,
            timeSetListener,
            cal.get(Calendar.HOUR_OF_DAY),
            cal.get(Calendar.MINUTE),
            true
        ).show()
    }

    companion object {
        fun newInstance(deviceId: String, streamName: String): ConfigureFragment {
            return ConfigureFragment().apply {
                arguments = Bundle().apply {
                    putString(DEVICE_ID, deviceId)
                    putString(ConfigureActivity.STREAM_NAME, streamName)
                }
            }
        }
    }
}
