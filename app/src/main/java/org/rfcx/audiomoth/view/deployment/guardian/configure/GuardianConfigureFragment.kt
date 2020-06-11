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
import androidx.fragment.app.Fragment
import kotlinx.android.synthetic.main.fragment_guardian_configure.*
import org.rfcx.audiomoth.R
import org.rfcx.audiomoth.view.deployment.guardian.GuardianDeploymentProtocol

class GuardianConfigureFragment : Fragment() {

    private var deploymentProtocol: GuardianDeploymentProtocol? = null

    private val sampleRateList = arrayOf("8", "16", "32", "48", "96", "192", "256", "384")
    private val bitrateList = arrayOf("8", "16", "32", "48", "96", "192", "256", "384")
    private val fileFormatList = arrayOf("OPUS", "FLAC")

    private var sampleRate = 8      // default sampleRate is 8
    private var bitrate = 0
    private var fileFormat = ""

    override fun onAttach(context: Context) {
        super.onAttach(context)
        deploymentProtocol = context as GuardianDeploymentProtocol
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_guardian_configure, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        deploymentProtocol?.hideCompleteButton()

        setNextButton(true)
        setFileFormatLayout()
        setSampleRateLayout()
        setBitrateLayout()
        createNotificationChannel()
        setNextOnClick()
    }

    private fun setNextButton(show: Boolean) {
        nextButton.visibility = if (show) View.VISIBLE else View.GONE
        configProgressBar.visibility = if (!show) View.VISIBLE else View.GONE
    }

    private fun setNextOnClick() {
        nextButton.setOnClickListener {
            setNextButton(false)
            updateProfile()
        }
    }

    private fun updateProfile() {
        val profileName = profileEditText.text

        deploymentProtocol?.setDeploymentConfigure()
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

        //TODO: get data from db
        bitrateValueTextView.text = "14kbs"

        bitrateValueTextView.setOnClickListener {
            val builder = context?.let { it1 -> AlertDialog.Builder(it1) }
            if (builder != null) {
                builder.setTitle(R.string.choose_bitrate)
                    ?.setItems(bitrateList) { dialog, i ->
                        try {
                            bitrateValueTextView.text = bitrateList[i]
                            bitrate = bitrateList[i].toInt()
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

        //TODO: get data from db
        fileFormatValueTextView.text = "OPUS"

        fileFormatValueTextView.setOnClickListener {
            val builder = context?.let { it1 -> AlertDialog.Builder(it1) }
            if (builder != null) {
                builder.setTitle(R.string.choose_file_format)
                    ?.setItems(fileFormatList) { dialog, i ->
                        try {
                            fileFormatValueTextView.text = fileFormatList[i]
                            fileFormat = fileFormatList[i]
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

        //TODO: get data from db
        sampleRateValueTextView.text = getString(R.string.kilohertz, "14")

        sampleRateValueTextView.setOnClickListener {
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

    companion object {
        const val CHANNEL_ID = "Guardian Notification"
        const val CHANNEL_NAME = "Notification"

        fun newInstance(): GuardianConfigureFragment {
            return GuardianConfigureFragment()
        }
    }
}