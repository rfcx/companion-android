package org.rfcx.audiomoth.view.configure


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

class ConfigureFragment : Fragment() {

    // Todo: save array in string.xml
    private val sampleRateList = arrayOf("8", "16", "32", "48", "96", "192", "256", "384")
    private val gainList = arrayOf("1 - Low...?", "2 - Low", "3 - Medium", "4 - High", "5 - High...?")

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_configure, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setSampleRateLayout()
        setGainLayout()
        setSleepDurationLayout()
        setRecordingDurationLayout()
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
}
