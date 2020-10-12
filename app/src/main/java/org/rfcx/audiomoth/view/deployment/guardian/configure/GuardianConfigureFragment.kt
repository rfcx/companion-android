package org.rfcx.audiomoth.view.deployment.guardian.configure

import android.app.AlertDialog
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.preference.PreferenceManager
import com.google.gson.JsonArray
import kotlinx.android.synthetic.main.fragment_guardian_configure.*
import org.rfcx.audiomoth.R
import org.rfcx.audiomoth.connection.socket.SocketManager
import org.rfcx.audiomoth.entity.Screen
import org.rfcx.audiomoth.entity.guardian.GuardianConfiguration
import org.rfcx.audiomoth.entity.guardian.GuardianProfile
import org.rfcx.audiomoth.entity.guardian.toListForGuardian
import org.rfcx.audiomoth.entity.socket.response.Status
import org.rfcx.audiomoth.util.Analytics
import org.rfcx.audiomoth.view.deployment.guardian.GuardianDeploymentProtocol
import org.rfcx.audiomoth.view.prefs.GuardianPrefsFragment
import org.rfcx.audiomoth.view.prefs.SyncPreferenceListener

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

    private var profile: GuardianProfile? = null

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
            it.showToolbar()
            it.setToolbarTitle()
        }

        profile = deploymentProtocol?.getProfile()

        setNextButton(true)
        setFileFormatLayout()
        setSampleRateLayout()
        setBitrateLayout()
        setDuration()
        createNotificationChannel()
        setNextOnClick()
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
            setNextButton(false)
            syncConfig()
            updateProfile()
            deploymentProtocol?.setSampleRate(sampleRate)
        }
    }

    private fun syncConfig() {
        SocketManager.syncConfiguration(getConfiguration().toListForGuardian())
        SocketManager.syncConfiguration.observe(viewLifecycleOwner, Observer {
            requireActivity().runOnUiThread {
                if (it.sync.status == Status.SUCCESS.value) {
                    deploymentProtocol?.nextStep()
                }
            }
        })
    }

    private fun updateProfile() {
        val profileName = profileEditText.text
        val profileTemp = GuardianProfile(
            name = profileName?.trim()?.toString() ?: "",
            sampleRate = sampleRate,
            bitrate = bitrate,
            fileFormat = fileFormat,
            duration = duration

        )

        val newProfile = profile?.let {
            if (it.name == profileTemp.name) {
                profileTemp.id = it.id
            }
            profileTemp
        } ?: kotlin.run {
            profileTemp
        }

        deploymentProtocol?.setDeploymentConfigure(newProfile)
    }

    private fun getConfiguration(): GuardianConfiguration {
        return GuardianConfiguration(sampleRate, bitrate, fileFormat, duration)
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

    private fun setBitrateLayout() {

        if (profile != null) {
            bitrate = profile?.bitrate ?: 28672
        }
        val indexOfValue = bitrateValues?.indexOf(bitrate.toString()) ?: 6
        bitrateValueTextView.text = bitrateEntries!![indexOfValue]

        bitrateValueTextView.setOnClickListener {
            val builder = context?.let { it1 -> AlertDialog.Builder(it1) }
            if (builder != null) {
                builder.setTitle(R.string.choose_bitrate)
                    ?.setItems(bitrateEntries) { dialog, i ->
                        try {
                            bitrateValueTextView.text = bitrateEntries!![i]
                            bitrate = bitrateValues!![i].toInt()
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

        if (profile != null) {
            fileFormat = profile?.fileFormat ?: "opus"
        }
        fileFormatValueTextView.text = fileFormat

        fileFormatValueTextView.setOnClickListener {
            val builder = context?.let { it1 -> AlertDialog.Builder(it1) }
            if (builder != null) {
                builder.setTitle(R.string.choose_file_format)
                    ?.setItems(fileFormatList) { dialog, i ->
                        try {
                            fileFormatValueTextView.text = fileFormatList!![i]
                            fileFormat = fileFormatList!![i]
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

        if (profile != null) {
            sampleRate = profile?.sampleRate ?: 24000
        }
        val indexOfValue = sampleRateValues?.indexOf(sampleRate.toString()) ?: 3
        sampleRateValueTextView.text = sampleRateEntries!![indexOfValue]

        sampleRateValueTextView.setOnClickListener {
            val builder = context?.let { it1 -> AlertDialog.Builder(it1) }
            if (builder != null) {
                builder.setTitle(R.string.choose_sample_rate)
                    ?.setItems(sampleRateEntries) { dialog, i ->
                        try {
                            sampleRateValueTextView.text = sampleRateEntries!![i]
                            sampleRate = sampleRateValues!![i].toInt()
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
        if (profile != null) {
            duration = profile?.duration ?: 90
        }
        val indexOfValue = durationValues?.indexOf(duration.toString()) ?: 3
        durationValueTextView.text = durationEntries!![indexOfValue]

        durationValueTextView.setOnClickListener {
            val builder = context?.let { it1 -> AlertDialog.Builder(it1) }
            if (builder != null) {
                builder.setTitle(R.string.choose_duration_cycle)
                    ?.setItems(durationEntries) { dialog, i ->
                        try {
                            durationValueTextView.text = durationEntries!![i]
                            duration = durationValues!![i].toInt()
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
        const val CHANNEL_ID = "Guardian Notification"
        const val CHANNEL_NAME = "Notification"

        fun newInstance(): GuardianConfigureFragment {
            return GuardianConfigureFragment()
        }
    }
}
