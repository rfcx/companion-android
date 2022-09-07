package org.rfcx.companion.view.deployment.guardian.microphone

import android.app.AlertDialog
import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.os.CountDownTimer
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import kotlinx.android.synthetic.main.fragment_guardian_microphone.*
import org.rfcx.companion.R
import org.rfcx.companion.connection.socket.AudioCastSocketManager
import org.rfcx.companion.connection.socket.GuardianSocketManager
import org.rfcx.companion.entity.Screen
import org.rfcx.companion.util.Analytics
import org.rfcx.companion.util.MicrophoneTestUtils
import org.rfcx.companion.util.spectrogram.AudioSpectrogramUtils
import org.rfcx.companion.util.spectrogram.SpectrogramListener
import org.rfcx.companion.util.spectrogram.toShortArray
import org.rfcx.companion.util.spectrogram.toSmallChunk
import org.rfcx.companion.view.deployment.guardian.GuardianDeploymentProtocol
import java.util.*

class GuardianMicrophoneFragment : Fragment(), SpectrogramListener {

    private val analytics by lazy { context?.let { Analytics(it) } }
    private var spectrogramTimer: Timer? = null
    private var socketTimer: CountDownTimer? = null
    private val spectrogramStack = arrayListOf<FloatArray>()
    private var isTimerPause = false

    private lateinit var dialogBuilder: AlertDialog

    private var deploymentProtocol: GuardianDeploymentProtocol? = null
    private val microphoneTestUtils by lazy {
        MicrophoneTestUtils()
    }

    private var isMicTesting = false

    private var nullStackThreshold = 0

    override fun onAttach(context: Context) {
        super.onAttach(context)
        deploymentProtocol = (context as GuardianDeploymentProtocol)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_guardian_microphone, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        deploymentProtocol?.let {
            it.setToolbarSubtitle()
            it.setMenuToolbar(false)
            it.showToolbar()
            it.setToolbarTitle()
        }

        checkTestingRequirement()

        setupAudioTrack()
        setupSpectrogram()
        setupSpectrogramSpeed()
        setupSpectrogramFreqMenu()
        setupSpectrogramColorMenu()
        setupAudioPlaybackMenu()
        setUiByState(MicTestingState.READY)
        AudioCastSocketManager.resetMicrophoneDefaultValue()

        listenAudioButton.setOnClickListener {
            isMicTesting = true
            setUiByState(MicTestingState.LISTENING)
            retrieveLiveAudioBuffer()
        }

        cancelAudioButton.setOnClickListener {
            isMicTesting = false
            isTimerPause = true
            setUiByState(MicTestingState.FINISH)
            spectrogramStack.clear()
            microphoneTestUtils.stop()
            stopSocketTimer()
        }

        listenAgainAudioButton.setOnClickListener {
            isMicTesting = true
            isTimerPause = false
            setUiByState(MicTestingState.LISTENING)
            microphoneTestUtils.play()
            scheduleSocketTimer()
        }

        finishButton.setOnClickListener {
            analytics?.trackClickNextEvent(Screen.GUARDIAN_MICROPHONE.id)
            deploymentProtocol?.nextStep()
        }
    }

    private fun checkTestingRequirement() {
        deploymentProtocol?.getAudioCapturing()?.let {
            if (!it.isCapturing) {
                showAlert(it.msg ?: getString(R.string.unknown_error))
            }
        }
    }

    private fun showAlert(text: String) {
        val dialogBuilder: AlertDialog.Builder =
            AlertDialog.Builder(requireContext()).apply {
                setTitle(null)
                setMessage(text)
                setPositiveButton(R.string.ok) { _, _ -> }
            }
        dialogBuilder.create().show()
    }

    private fun setupAudioTrack() {
        microphoneTestUtils.setSampleRate(deploymentProtocol?.getSampleRate() ?: DEF_SAMPLERATE)
    }

    private fun setupSpectrogram() {
        AudioSpectrogramUtils.resetToDefaultValue()
        spectrogramView.resetToDefaultValue()
        spectrogramView.setSamplingRate(deploymentProtocol?.getSampleRate() ?: DEF_SAMPLERATE)
        spectrogramView.setBackgroundColor(Color.BLACK)
    }

    private fun setupSpectrogramSpeed() {
        speedValueTextView.text = speed[0]
        speedValueTextView.setOnClickListener {
            val builder = context?.let { it1 -> AlertDialog.Builder(it1, R.style.DialogCustom) }
            if (builder != null) {
                builder.setTitle(R.string.choose_speed)
                    ?.setItems(speed) { dialog, i ->
                        try {
                            speedValueTextView.text = speed[i]
                            AudioSpectrogramUtils.setSpeed(speed[i])
                            AudioSpectrogramUtils.resetSetupState()
                            spectrogramStack.clear()
                            spectrogramView.invalidate()
                        } catch (e: IllegalArgumentException) {
                            dialog.dismiss()
                        }
                    }
                val dialog = builder.create()
                dialog.show()
            }
        }
    }

    private fun setupSpectrogramFreqMenu() {
        freqScaleValueTextView.text = freq[0]
        freqScaleValueTextView.setOnClickListener {
            val builder = context?.let { it1 -> AlertDialog.Builder(it1, R.style.DialogCustom) }
            if (builder != null) {
                builder.setTitle(R.string.choose_freq)
                    ?.setItems(freq) { dialog, i ->
                        try {
                            freqScaleValueTextView.text = freq[i]
                            spectrogramView.freqScale = freq[i]
                            spectrogramView.invalidate()
                        } catch (e: IllegalArgumentException) {
                            dialog.dismiss()
                        }
                    }
                val dialog = builder.create()
                dialog.show()
            }
        }
    }

    private fun setupSpectrogramColorMenu() {
        colorSpecValueTextView.text = color[0]
        colorSpecValueTextView.setOnClickListener {
            val builder = context?.let { it1 -> AlertDialog.Builder(it1, R.style.DialogCustom) }
            if (builder != null) {
                builder.setTitle(R.string.choose_color)
                    ?.setItems(color) { dialog, i ->
                        try {
                            colorSpecValueTextView.text = color[i]
                            spectrogramView.colorScale = color[i]
                            spectrogramView.invalidate()
                        } catch (e: IllegalArgumentException) {
                            dialog.dismiss()
                        }
                    }
                val dialog = builder.create()
                dialog.show()
            }
        }
    }

    private fun setupAudioPlaybackMenu() {
        playbackValueTextView.text = playback[0]
        playbackValueTextView.setOnClickListener {
            val builder = context?.let { it1 -> AlertDialog.Builder(it1, R.style.DialogCustom) }
            if (builder != null) {
                builder.setTitle(R.string.choose_play_back)
                    ?.setItems(playback) { dialog, i ->
                        try {
                            playbackValueTextView.text = playback[i]
                            if (i == 0) {
                                microphoneTestUtils.play()
                            } else {
                                microphoneTestUtils.stop()
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

    private fun setUiByState(state: MicTestingState) {
        when (state) {
            MicTestingState.READY -> {
                listenAudioButton.visibility = View.VISIBLE
                cancelAudioButton.visibility = View.GONE
                listenAgainAudioButton.visibility = View.GONE
                finishButton.visibility = View.GONE
                microphoneView.setBackgroundResource(R.drawable.ic_microphone_grey)
            }
            MicTestingState.LISTENING -> {
                listenAudioButton.visibility = View.GONE
                cancelAudioButton.visibility = View.VISIBLE
                listenAgainAudioButton.visibility = View.GONE
                finishButton.visibility = View.GONE
                microphoneView.setBackgroundResource(R.drawable.ic_microphone_green)
            }
            MicTestingState.FINISH -> {
                listenAudioButton.visibility = View.GONE
                cancelAudioButton.visibility = View.GONE
                listenAgainAudioButton.visibility = View.VISIBLE
                finishButton.visibility = View.VISIBLE
                microphoneView.setBackgroundResource(R.drawable.ic_microphone_grey)
            }
        }
    }

    private fun retrieveLiveAudioBuffer() {
        spectrogramTimer = Timer()

        AudioCastSocketManager.connect(microphoneTestUtils)

        spectrogramTimer?.schedule(
            object : TimerTask() {
                override fun run() {
                    if (!spectrogramStack.isNullOrEmpty()) {
                        nullStackThreshold = 0
                        try {
                            spectrogramView.setMagnitudes(spectrogramStack[0] ?: FloatArray(0))
                            spectrogramView.invalidate()
                            spectrogramStack.removeAt(0)
                        } catch (e: Exception) { /* nothing now */ }
                    } else {
                        nullStackThreshold++
                        if (nullStackThreshold >= 50) {
                            nullStackThreshold = 0
                            AudioCastSocketManager.connect(microphoneTestUtils)
                        }
                    }
                }
            },
            DELAY, STACK_PERIOD
        )

        setDialog()
        scheduleSocketTimer()

        AudioCastSocketManager.spectrogram.observe(
            viewLifecycleOwner,
            Observer {
                if (isMicTesting) {
                    if (it.size > 2) {
                        AudioSpectrogramUtils.setupSpectrogram(it.size)
                        val audioChunks = it.toShortArray().toSmallChunk(1)
                        for (chunk in audioChunks) {
                            AudioSpectrogramUtils.getTrunks(chunk, this)
                        }
                        stopSocketTimer()
                        scheduleSocketTimer()
                    }
                }
            }
        )
    }

    private fun setDialog() {
        dialogBuilder =
            AlertDialog.Builder(requireContext()).apply {
                setTitle(null)
                setMessage(R.string.dialog_start_service_mic)
                setPositiveButton(R.string.restart) { _, _ ->
                    GuardianSocketManager.restartService("audio-cast-socket")
                }
                setNegativeButton(R.string.cancel) { _, _ ->
                    dialogBuilder.dismiss()
                }
            }.create()
    }

    private fun scheduleSocketTimer() {
        socketTimer = object : CountDownTimer(10000, 1000) {
            override fun onTick(millisUntilFinished: Long) { }

            override fun onFinish() {
                showRestartGuardianServices()
                stopSocketTimer()
            }
        }
        socketTimer?.start()
    }

    private fun stopSocketTimer() {
        socketTimer?.cancel()
        socketTimer = null
    }

    override fun onProcessed(mag: FloatArray) {
        spectrogramStack.add(mag)
    }

    private fun showRestartGuardianServices() {
        if (::dialogBuilder.isInitialized && dialogBuilder.isShowing) {
            return
        }
        dialogBuilder.show()
    }

    override fun onDetach() {
        super.onDetach()
        microphoneTestUtils.let {
            it.stop()
            it.release()
        }
        spectrogramTimer?.cancel()
        spectrogramTimer = null

        stopSocketTimer()

        if (isMicTesting) {
            isMicTesting = false
        }
        AudioCastSocketManager.resetAllValuesToDefault()
        AudioCastSocketManager.stopConnection()
    }

    override fun onResume() {
        super.onResume()
        analytics?.trackScreen(Screen.GUARDIAN_MICROPHONE)
    }

    companion object {

        private val color = arrayOf("Rainbow", "Fire", "Ice", "Grey")
        private val freq = arrayOf("Linear", "Logarithmic")
        private val speed = arrayOf("Fast", "Normal", "Slow")
        private val playback = arrayOf("On", "Off")

        private const val DELAY = 0L

        private const val STACK_PERIOD = 10L

        private const val SOCKET_PERIOD = 120000L

        private const val DEF_SAMPLERATE = 12000

        enum class MicTestingState { READY, LISTENING, FINISH }

        fun newInstance(): GuardianMicrophoneFragment = GuardianMicrophoneFragment()
    }
}
