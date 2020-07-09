package org.rfcx.audiomoth.view.deployment.guardian.verify

import android.content.Context
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import kotlinx.android.synthetic.main.fragment_guardian_verify.*
import org.rfcx.audiomoth.R
import org.rfcx.audiomoth.connection.socket.OnReceiveResponse
import org.rfcx.audiomoth.connection.socket.SocketManager
import org.rfcx.audiomoth.entity.socket.SignalResponse
import org.rfcx.audiomoth.entity.socket.SocketResposne
import org.rfcx.audiomoth.view.deployment.guardian.GuardianDeploymentProtocol
import java.util.*

class GuardianVerifyFragment : Fragment(), OnReceiveResponse {
    private val listOfSignal by lazy {
        listOf(signalStrength1, signalStrength2, signalStrength3, signalStrength4)
    }

    private val timer by lazy {
        Timer()
    }

    private var deploymentProtocol: GuardianDeploymentProtocol? = null

    override fun onAttach(context: Context) {
        super.onAttach(context)
        deploymentProtocol = (context as GuardianDeploymentProtocol)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_guardian_verify, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        deploymentProtocol?.showLoading()
        retrieveGuardianSignal()

        finishButton.setOnClickListener {
            deploymentProtocol?.nextStep()
        }
    }

    private fun retrieveGuardianSignal() {
        timer.schedule(object : TimerTask(){
            override fun run() {
                SocketManager.getSignalStrength(this@GuardianVerifyFragment)
            }
        }, DELAY, MILLI_PERIOD)
    }

    private fun showSignalStrength(state: SignalState) {
        listOfSignal.forEachIndexed { index, view ->
            if (index < state.value){
                (view.background as GradientDrawable).setBackground(requireContext(), R.color.signal_filled)
            } else {
                (view.background as GradientDrawable).setBackground(requireContext(), R.color.white)
            }
        }
    }

    override fun onReceive(response: SocketResposne) {
        deploymentProtocol?.hideLoading()
        val signalResponse = response as SignalResponse
        val strength = signalResponse.signal
        requireActivity().runOnUiThread {
            when {
                strength > -70 -> {
                    showSignalStrength(SignalState.MAX)
                    signalDescText.text = getString(R.string.signal_text_4)
                }
                strength > -90 -> {
                    showSignalStrength(SignalState.HIGH)
                    signalDescText.text = getString(R.string.signal_text_3)
                }
                strength > -110 -> {
                    showSignalStrength(SignalState.NORMAL)
                    signalDescText.text = getString(R.string.signal_text_2)
                }
                strength > -130 -> {
                    showSignalStrength(SignalState.LOW)
                    signalDescText.text = getString(R.string.signal_text_1)
                }
                else -> {
                    showSignalStrength(SignalState.NONE)
                    signalDescText.text = getString(R.string.signal_text_0)
                }
            }
            signalValue.text = getString(R.string.signal_value, strength)
        }
    }

    override fun onFailed(message: String) {
        requireActivity().runOnUiThread {
            Toast.makeText(requireContext(), "Getting signal failed", Toast.LENGTH_LONG).show()
        }
    }

    override fun onDetach() {
        super.onDetach()
        timer.cancel()
    }

    companion object {
        private const val DELAY = 0L
        private const val MILLI_PERIOD = 1000L

        private enum class SignalState(val value: Int) {
            NONE(0), LOW(1), NORMAL(2), HIGH(3), MAX(4)
        }

        private fun GradientDrawable.setBackground(context: Context, color: Int) {
            this.setColor(ContextCompat.getColor(context, color))
        }

        fun newInstance(): GuardianVerifyFragment = GuardianVerifyFragment()
    }
}
