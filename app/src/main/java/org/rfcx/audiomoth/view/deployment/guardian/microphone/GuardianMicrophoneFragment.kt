package org.rfcx.audiomoth.view.deployment.guardian.microphone

import android.app.AlertDialog
import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import kotlinx.android.synthetic.main.fragment_guardian_microphone.*
import org.rfcx.audiomoth.R
import org.rfcx.audiomoth.connection.socket.SocketManager
import org.rfcx.audiomoth.util.MicrophoneTestUtils
import org.rfcx.audiomoth.util.spectrogram.AudioSpectrogramUtils
import org.rfcx.audiomoth.util.spectrogram.SpectrogramListener
import org.rfcx.audiomoth.util.spectrogram.toShortArray
import org.rfcx.audiomoth.util.spectrogram.toSmallChunk
import org.rfcx.audiomoth.view.deployment.guardian.GuardianDeploymentProtocol
import java.util.*


class GuardianMicrophoneFragment : Fragment(), SpectrogramListener {

    private var timer: Timer? = null
    private var spectrogramTimer: Timer? = null
    private val spectrogramStack = arrayListOf<FloatArray>()
    private var isTimerPause = false

    private var deploymentProtocol: GuardianDeploymentProtocol? = null
    private val microphoneTestUtils by lazy {
        MicrophoneTestUtils()
    }

    private var isMicTesting = false

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

        deploymentProtocol?.hideCompleteButton()
        setupAudioTrack()
        setupSpectrogram()
        setupSpectrogramSpeed()
        setupSpectrogramFreqMenu()
        setupSpectrogramColorMenu()
        setupAudioPlaybackMenu()
        setUiByState(MicTestingState.READY)
        SocketManager.resetDefaultValue()

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
        }

        listenAgainAudioButton.setOnClickListener {
            isMicTesting = true
            isTimerPause = false
            setUiByState(MicTestingState.LISTENING)
            microphoneTestUtils.play()
        }

        finishButton.setOnClickListener {
            deploymentProtocol?.nextStep()
        }
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
            val builder = context?.let { it1 -> AlertDialog.Builder(it1) }
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
            val builder = context?.let { it1 -> AlertDialog.Builder(it1) }
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
            val builder = context?.let { it1 -> AlertDialog.Builder(it1) }
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
            val builder = context?.let { it1 -> AlertDialog.Builder(it1) }
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
        timer = Timer()
        spectrogramTimer = Timer()

        timer?.schedule(object : TimerTask() {
            override fun run() {
                if (!isTimerPause && isMicTesting) {
                    SocketManager.getLiveAudioBuffer(microphoneTestUtils)
                    isTimerPause = true
                }
            }
        }, DELAY, MILLI_PERIOD)

        spectrogramTimer?.schedule(object : TimerTask() {
            override fun run() {
                if (spectrogramStack.isNotEmpty()) {
                    if (spectrogramStack[0] != null) {
                        spectrogramView.setMagnitudes(spectrogramStack[0])
                        spectrogramView.invalidate()
                        spectrogramStack.removeAt(0)
                    }
                }
            }
        }, DELAY, STACK_PERIOD)

        SocketManager.liveAudio.observe(viewLifecycleOwner, Observer {
            isTimerPause = false
        })

        SocketManager.spectrogram.observe(viewLifecycleOwner, Observer {
            if (it.size > 2) {
                AudioSpectrogramUtils.setupSpectrogram(it.size)
                val audioChunks = it.toShortArray().toSmallChunk(1)
                for (chunk in audioChunks) {
                    AudioSpectrogramUtils.getTrunks(chunk, this)
                }
            }
        })
    }

    override fun onProcessed(mag: FloatArray) {
        spectrogramStack.add(mag)
    }

    override fun onDetach() {
        super.onDetach()
        microphoneTestUtils.let {
            it.stop()
            it.release()
        }
        spectrogramTimer?.cancel()
        spectrogramTimer = null

        if (isMicTesting) {
            timer?.cancel()
            timer = null
            isMicTesting = false
        }
    }

    companion object {

        private val color = arrayOf("Rainbow", "Fire", "Ice", "Grey")
        private val freq = arrayOf("Linear", "Logarithmic")
        private val speed = arrayOf("Fast", "Normal", "Slow")
        private val playback = arrayOf("On", "Off")

        private const val DELAY = 0L
        private const val MILLI_PERIOD = 10L

        private const val STACK_PERIOD = 10L

        private const val DEF_SAMPLERATE = 24000

        enum class MicTestingState { READY, LISTENING, FINISH }

        fun newInstance(): GuardianMicrophoneFragment = GuardianMicrophoneFragment()
    }

}
