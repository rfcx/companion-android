package org.rfcx.companion.view.deployment.guardian.signal

import android.content.Context
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import java.util.*
import kotlinx.android.synthetic.main.fragment_guardian_signal.*
import org.rfcx.companion.R
import org.rfcx.companion.connection.socket.SocketManager
import org.rfcx.companion.entity.Screen
import org.rfcx.companion.util.Analytics
import org.rfcx.companion.view.deployment.guardian.GuardianDeploymentProtocol

class GuardianSignalFragment : Fragment() {
    private val listOfSignal by lazy {
        listOf(signalStrength1, signalStrength2, signalStrength3, signalStrength4)
    }

    private var timer: Timer? = null

    private var isSignalTesting = false

    private var deploymentProtocol: GuardianDeploymentProtocol? = null

    private val analytics by lazy { context?.let { Analytics(it) } }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        deploymentProtocol = (context as GuardianDeploymentProtocol)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_guardian_signal, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        deploymentProtocol?.let {
            it.showToolbar()
            it.setToolbarTitle()
        }

        deploymentProtocol?.showLoading()
        retrieveGuardianSignal()

        finishButton.setOnClickListener {
            deploymentProtocol?.nextStep()
        }
    }

    private fun retrieveGuardianSignal() {
        isSignalTesting = true

        timer = Timer()
        timer?.schedule(object : TimerTask() {
            override fun run() {
                SocketManager.getSignalStrength()
            }
        }, DELAY, MILLI_PERIOD)

        SocketManager.signal.observe(viewLifecycleOwner, Observer { signal ->
            deploymentProtocol?.hideLoading()
            val strength = signal.signalInfo.signal
            val simCard = signal.signalInfo.simCard
            requireActivity().runOnUiThread {
                if (simCard) {
                    hideSimError()
                    showSignalInfo()
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
                } else {
                    hideSignalInfo()
                    showSimError()
                    showSignalStrength(SignalState.NONE)
                    if (strength == -999) {
                        signalErrorText.text = getText(R.string.signal_lost)
                    } else {
                        signalErrorText.text = getText(R.string.signal_sim_card)
                    }
                }
            }
        })
    }

    private fun showSignalStrength(state: SignalState) {
        listOfSignal.forEachIndexed { index, view ->
            if (index < state.value) {
                (view.background as GradientDrawable).setBackground(
                    requireContext(),
                    R.color.signal_filled
                )
            } else {
                (view.background as GradientDrawable).setBackground(requireContext(), R.color.white)
            }
        }
    }

    private fun showSignalInfo() {
        signalDescText.visibility = View.VISIBLE
        signalValue.visibility = View.VISIBLE
    }

    private fun hideSignalInfo() {
        signalDescText.visibility = View.GONE
        signalValue.visibility = View.GONE
    }

    private fun showSimError() {
        signalErrorText.visibility = View.VISIBLE
    }

    private fun hideSimError() {
        signalErrorText.visibility = View.GONE
    }

    override fun onDetach() {
        super.onDetach()
        if (isSignalTesting) {
            timer?.cancel()
            timer = null
        }
    }

    override fun onResume() {
        super.onResume()
        analytics?.trackScreen(Screen.GUARDIAN_SIGNAL)
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

        fun newInstance(): GuardianSignalFragment = GuardianSignalFragment()
    }
}
