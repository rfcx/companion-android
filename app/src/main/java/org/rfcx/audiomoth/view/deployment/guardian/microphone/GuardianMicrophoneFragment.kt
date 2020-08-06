package org.rfcx.audiomoth.view.deployment.guardian.microphone

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
import org.rfcx.audiomoth.util.spectrogram.toShortArray
import org.rfcx.audiomoth.view.deployment.guardian.GuardianDeploymentProtocol
import java.util.*

class GuardianMicrophoneFragment : Fragment() {

    private var timer: Timer? = null
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
        setupSpectrogram()
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
        spectrogramView.setFFTResolution(AudioSpectrogramUtils.fftResolution)
        spectrogramView.setSamplingRate(44100)
        spectrogramView.setBackgroundColor(Color.BLACK)
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

        timer?.schedule( object : TimerTask(){
            override fun run() {
                if (!isTimerPause) {
                    SocketManager.getLiveAudioBuffer(microphoneTestUtils)
                    isTimerPause = true
                }
            }
        }, DELAY, MILLI_PERIOD)

        SocketManager.liveAudio.observe(viewLifecycleOwner, Observer {
            isTimerPause = false
        })

        SocketManager.spectrogram.observe(viewLifecycleOwner, Observer {
            if (it.size > 2) {
                AudioSpectrogramUtils.setupSpectrogram(it.size)
                AudioSpectrogramUtils.getTrunks(it.toShortArray())
            }
        })

        AudioSpectrogramUtils.spectrogramLive.observe(viewLifecycleOwner, Observer {
            if (it.size > 2) {
                spectrogramView.setMagnitudes(it)
                requireActivity().runOnUiThread {
                    spectrogramView.invalidate()
                }
            }
        })
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
    }

    companion object {
        private const val DELAY = 0L
        private const val MILLI_PERIOD = 10L

        enum class MicTestingState { READY, LISTENING, FINISH }

        fun newInstance(): GuardianMicrophoneFragment = GuardianMicrophoneFragment()
    }

}
