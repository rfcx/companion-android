package org.rfcx.audiomoth.view.deployment.guardian.microphone

import android.content.Context
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import kotlinx.android.synthetic.main.fragment_guardian_microphone.*
import org.rfcx.audiomoth.R
import org.rfcx.audiomoth.connection.socket.OnReceiveResponse
import org.rfcx.audiomoth.connection.socket.SocketManager
import org.rfcx.audiomoth.entity.socket.MicrophoneTestResponse
import org.rfcx.audiomoth.entity.socket.SocketResposne
import org.rfcx.audiomoth.util.MicrophoneTestUtils
import org.rfcx.audiomoth.view.deployment.guardian.GuardianDeploymentProtocol
import org.rfcx.audiomoth.view.deployment.guardian.signal.GuardianSignalFragment
import java.net.Socket
import java.util.*

class GuardianMicrophoneFragment : Fragment(), OnReceiveResponse {

    private var deploymentProtocol: GuardianDeploymentProtocol? = null

    private val timer by lazy {
        Timer()
    }
    private var timerPause: Boolean = false

    private val microphoneTestUtils by lazy {
        MicrophoneTestUtils()
    }

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
        setUiByState(MicTestingState.READY)

        microphoneTestUtils.init()
        microphoneTestUtils.play()

        listenAudioButton.setOnClickListener {
            setUiByState(MicTestingState.LISTENING)
            retrieveLiveAudioBuffer()
        }

        cancelAudioButton.setOnClickListener {
            setUiByState(MicTestingState.FINISH)
            retrieveLiveAudioBuffer()
            microphoneTestUtils.stop()
            timerPause = true
        }

        listenAgainAudioButton.setOnClickListener {
            setUiByState(MicTestingState.LISTENING)
            microphoneTestUtils.play()
            //retrieveLiveAudioBuffer()
            timerPause = false
        }

        finishButton.setOnClickListener {
            deploymentProtocol?.nextStep()
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
        SocketManager.getLiveAudioBuffer(
            microphoneTestUtils,
            this@GuardianMicrophoneFragment
        )
//        timer.schedule(object : TimerTask() {
//            override fun run() {
//                if (!timerPause) {
//                    SocketManager.getLiveAudioBuffer(
//                        microphoneTestUtils,
//                        this@GuardianMicrophoneFragment
//                    )
//                }
//            }
//        }, DELAY, MILLI_PERIOD)
    }

    override fun onReceive(response: SocketResposne) { /* not used */ }

    override fun onFailed(message: String) {
        requireActivity().runOnUiThread {
            Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show()
        }
    }

    override fun onDetach() {
        super.onDetach()
        microphoneTestUtils.let {
            it.stop()
            it.release()
        }
        timer.cancel()
    }

    companion object {
        private const val DELAY = 0L
        private const val MILLI_PERIOD = 1L

        enum class MicTestingState { READY, LISTENING, FINISH }

        fun newInstance(): GuardianMicrophoneFragment = GuardianMicrophoneFragment()
    }

}
