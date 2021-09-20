package org.rfcx.companion.view.deployment.guardian.configure

import android.app.AlertDialog
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import kotlinx.android.synthetic.main.fragment_guardian_configure.*
import org.rfcx.companion.R
import org.rfcx.companion.connection.socket.GuardianSocketManager
import org.rfcx.companion.entity.Screen
import org.rfcx.companion.entity.guardian.GuardianConfiguration
import org.rfcx.companion.entity.guardian.toListForGuardian
import org.rfcx.companion.util.Analytics
import org.rfcx.companion.util.prefs.PrefsUtils
import org.rfcx.companion.view.deployment.guardian.GuardianDeploymentProtocol

class GuardianConfigureFragment : Fragment() {
    private val analytics by lazy { context?.let { Analytics(it) } }
    private var deploymentProtocol: GuardianDeploymentProtocol? = null

    // Predefined configuration values
    private var sampleRateEntries: Array<String>? = null
    private var sampleRateValues: Array<String>? = null
    private var bitrateEntries: Array<String>? = null
    private var bitrateValues: Array<String>? = null
    private var fileFormatList: Array<String>? = null
    private var durationEntries: Array<String>? = null
    private var durationValues: Array<String>? = null

    private var sampleRate = 24000 // default guardian sampleRate is 24000
    private var bitrate = 28672 // default guardian bitrate is 28672
    private var fileFormat = "opus" // default guardian file format is opus
    private var duration = 90 // default guardian duration is 90

    private var needCheckSha1 = false
    private var currentPrefsSha1: String? = null


    override fun onAttach(context: Context) {
        super.onAttach(context)
        deploymentProtocol = context as GuardianDeploymentProtocol
        setPredefinedConfiguration(context)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_guardian_configure, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        deploymentProtocol?.let {
            it.setToolbarSubtitle()
            it.setMenuToolbar(false)
            it.showToolbar()
            it.setToolbarTitle()
            currentPrefsSha1 = it.getPrefsSha1()
        }

        setNextButton(true)
        retrieveCurrentConfigure()
    }

    private fun setPredefinedConfiguration(context: Context) {
        sampleRateEntries = context.resources.getStringArray(R.array.sample_rate_entries)
        sampleRateValues = context.resources.getStringArray(R.array.sample_rate_values)
        bitrateEntries = context.resources.getStringArray(R.array.bitrate_entries)
        bitrateValues = context.resources.getStringArray(R.array.bitrate_values)
        fileFormatList = context.resources.getStringArray(R.array.audio_codec)
        durationEntries = context.resources.getStringArray(R.array.duration_cycle_entries)
        durationValues = context.resources.getStringArray(R.array.duration_cycle_values)
    }

    private fun setNextButton(show: Boolean) {
        nextButton.visibility = if (show) View.VISIBLE else View.GONE
        configProgressBar.visibility = if (!show) View.VISIBLE else View.GONE
    }

    private fun setNextOnClick() {
        nextButton.setOnClickListener {
            analytics?.trackClickNextEvent(Screen.GUARDIAN_CONFIGURE.id)
            setNextButton(false)
            deploymentProtocol?.nextStep()
            syncConfig()
            deploymentProtocol?.setSampleRate(sampleRate)
        }
    }

    private fun syncConfig() {
        GuardianSocketManager.syncConfiguration(getConfiguration().toListForGuardian())
        GuardianSocketManager.syncConfiguration.observe(viewLifecycleOwner, Observer {
            requireActivity().runOnUiThread {
                if (!needCheckSha1) {
                    deploymentProtocol?.nextStep()
                }
                if (currentPrefsSha1 != deploymentProtocol?.getPrefsSha1()) {
                    deploymentProtocol?.nextStep()
                }
            }
        })
    }

    private fun getConfiguration(): GuardianConfiguration {
        return GuardianConfiguration(sampleRate, bitrate, fileFormat, duration)
    }

    private fun retrieveCurrentConfigure() {
        deploymentProtocol?.getAudioConfiguration()?.let {
            bitrate = it.get(PrefsUtils.audioBitrate).asInt
            sampleRate = it.get(PrefsUtils.audioSampleRate).asInt
            duration = it.get(PrefsUtils.audioDuration).asInt
            fileFormat = it.get(PrefsUtils.audioCodec).asString
        }
        setFileFormatLayout()
        setSampleRateLayout()
        setBitrateLayout()
        setDuration()
        setNextOnClick()
    }

    private fun setBitrateLayout() {

        val indexOfValue = bitrateValues?.indexOf(bitrate.toString()) ?: 6
        bitrateValueTextView.text = bitrateEntries!![indexOfValue]

        bitrateValueTextView.setOnClickListener {
            val builder = context?.let { it1 -> AlertDialog.Builder(it1, R.style.DialogCustom) }
            if (builder != null) {
                builder.setTitle(R.string.choose_bitrate)
                    ?.setItems(bitrateEntries) { dialog, i ->
                        try {
                            if (bitrateValues!![i].toInt() == bitrate) {
                                needCheckSha1 = false
                            } else {
                                bitrateValueTextView.text = bitrateEntries!![i]
                                bitrate = bitrateValues!![i].toInt()
                                needCheckSha1 = true
                            }
                        } catch (e: IllegalArgumentException) {
                            dialog.dismiss()
                        }
                    }
                val dialog = builder.create()
                dialog.show()
            }
        }
    }

    private fun setFileFormatLayout() {

        fileFormatValueTextView.text = fileFormat

        fileFormatValueTextView.setOnClickListener {
            val builder = context?.let { it1 -> AlertDialog.Builder(it1, R.style.DialogCustom) }
            if (builder != null) {
                builder.setTitle(R.string.choose_file_format)
                    ?.setItems(fileFormatList) { dialog, i ->
                        try {
                            if (fileFormatList!![i] == fileFormat) {
                                needCheckSha1 = false
                            } else {
                                fileFormatValueTextView.text = fileFormatList!![i]
                                fileFormat = fileFormatList!![i]
                                needCheckSha1 = true
                            }
                        } catch (e: IllegalArgumentException) {
                            dialog.dismiss()
                        }
                    }
                val dialog = builder.create()
                dialog.show()
            }
        }
    }

    private fun setSampleRateLayout() {

        val indexOfValue = sampleRateValues?.indexOf(sampleRate.toString()) ?: 3
        sampleRateValueTextView.text = sampleRateEntries!![indexOfValue]

        sampleRateValueTextView.setOnClickListener {
            val builder = context?.let { it1 -> AlertDialog.Builder(it1, R.style.DialogCustom) }
            if (builder != null) {
                builder.setTitle(R.string.choose_sample_rate)
                    ?.setItems(sampleRateEntries) { dialog, i ->
                        try {
                            if (sampleRateValues!![i].toInt() == sampleRate) {
                                needCheckSha1 = false
                            } else {
                                sampleRateValueTextView.text = sampleRateEntries!![i]
                                sampleRate = sampleRateValues!![i].toInt()
                                needCheckSha1 = true
                            }
                        } catch (e: IllegalArgumentException) {
                            dialog.dismiss()
                        }
                    }
                val dialog = builder.create()
                dialog.show()
            }
        }
    }

    private fun setDuration() {

        val indexOfValue = durationValues?.indexOf(duration.toString()) ?: 3
        if (indexOfValue == -1) {
            durationValueTextView.text = "$duration secs"
        } else {
            durationValueTextView.text = durationEntries!![indexOfValue]
        }

        durationValueTextView.setOnClickListener {
            val builder = context?.let { it1 -> AlertDialog.Builder(it1, R.style.DialogCustom) }
            if (builder != null) {
                builder.setTitle(R.string.choose_duration_cycle)
                    ?.setItems(durationEntries) { dialog, i ->
                        try {
                            if (durationValues!![i].toInt() == duration) {
                                needCheckSha1 = false
                            } else {
                                durationValueTextView.text = durationEntries!![i]
                                duration = durationValues!![i].toInt()
                                needCheckSha1 = true
                            }
                        } catch (e: IllegalArgumentException) {
                            dialog.dismiss()
                        }
                    }
                val dialog = builder.create()
                dialog.show()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        analytics?.trackScreen(Screen.GUARDIAN_CONFIGURE)
    }

    companion object {
        fun newInstance(): GuardianConfigureFragment {
            return GuardianConfigureFragment()
        }
    }
}
