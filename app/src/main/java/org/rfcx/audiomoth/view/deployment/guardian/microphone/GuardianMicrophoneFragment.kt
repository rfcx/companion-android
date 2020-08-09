package org.rfcx.audiomoth.view.deployment.guardian.microphone

import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
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
import kotlin.collections.ArrayList


class GuardianMicrophoneFragment : Fragment(), SpectrogramListener {

    private var timer: Timer? = null
    private var spectrogramTimer: Timer? = null
    private val spectrogramStack = arrayListOf<FloatArray>()
    private var isTimerPause = false

    var x = 0
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
        setupSpectrogram()
        setupSpectrogramSpeed()
        setupSpectrogramFreqMenu()
        setupSpectrogramColorMenu()
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

    private fun setupSpectrogram() {
        spectrogramView.setSamplingRate(deploymentProtocol?.getSampleRate() ?: DEF_SAMPLERATE)
        spectrogramView.setBackgroundColor(Color.WHITE)
    }

    private fun setupSpectrogramSpeed() {
        val adapter: ArrayAdapter<String> = ArrayAdapter<String>(
            requireContext(),
            R.layout.dropdown_menu_popup_spectrogram,
            speed
        )
        speedSpecDropdown.setAdapter(adapter)
        speedSpecDropdown.setOnItemClickListener { _, _, position, _ ->
            AudioSpectrogramUtils.setSpeed(speed[position])
            AudioSpectrogramUtils.resetSetupState()
            spectrogramStack.clear()
            spectrogramView.invalidate()
        }
        speedSpecDropdown.inputType = 0
        speedSpecDropdown.setText(speed[0], false)
    }

    private fun setupSpectrogramFreqMenu() {
        val adapter: ArrayAdapter<String> = ArrayAdapter<String>(
            requireContext(),
            R.layout.dropdown_menu_popup_spectrogram,
            freq
        )
        freqScaleSpecDropdown.setAdapter(adapter)
        freqScaleSpecDropdown.setOnItemClickListener { _, _, position, _ ->
            spectrogramView.freqScale = freq[position]
            spectrogramView.invalidate()
        }
        freqScaleSpecDropdown.inputType = 0
        freqScaleSpecDropdown.setText(freq[0], false)
    }

    private fun setupSpectrogramColorMenu() {
        val adapter: ArrayAdapter<String> = ArrayAdapter<String>(
            requireContext(),
            R.layout.dropdown_menu_popup_spectrogram,
            color
        )
        colorSpecDropdown.setAdapter(adapter)
        colorSpecDropdown.setOnItemClickListener { _, _, position, _ ->
            spectrogramView.colorScale = color[position]
            spectrogramView.invalidate()
        }
        colorSpecDropdown.inputType = 0
        colorSpecDropdown.setText(color[0], false)
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

        timer?.schedule( object : TimerTask(){
            override fun run() {
                if (!isTimerPause && isMicTesting) {
                    SocketManager.getLiveAudioBuffer(microphoneTestUtils)
                    isTimerPause = true
                }
            }
        }, DELAY, MILLI_PERIOD)

        spectrogramTimer?.schedule( object : TimerTask() {
            override fun run() {
                if (spectrogramStack.isNotEmpty()) {
                    spectrogramView.setMagnitudes(spectrogramStack[0])
                    spectrogramView.invalidate()
                    spectrogramStack.removeAt(0)
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
        Log.d("audi", "add ${++x}")
        spectrogramStack.add(mag)
    }

    override fun onDetach() {
        super.onDetach()
        microphoneTestUtils.let {
            it.stop()
            it.release()
        }
        if (isMicTesting) {
            timer?.cancel()
            timer = null
        }
        spectrogramTimer?.cancel()
        spectrogramTimer= null
    }

    companion object {

        private val color = arrayOf("Rainbow", "Fire", "Ice", "Grey")
        private val freq = arrayOf("Linear", "Logarithmic")
        private val speed = arrayOf("Fast", "Normal", "Slow")

        private const val DELAY = 0L
        private const val MILLI_PERIOD = 10L

        private const val STACK_PERIOD = 40L
        
        private const val DEF_SAMPLERATE = 24000

        enum class MicTestingState { READY, LISTENING, FINISH }

        fun newInstance(): GuardianMicrophoneFragment = GuardianMicrophoneFragment()
    }

}
