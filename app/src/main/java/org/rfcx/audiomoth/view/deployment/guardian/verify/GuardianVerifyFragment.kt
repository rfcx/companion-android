package org.rfcx.audiomoth.view.deployment.guardian.verify

import android.content.Context
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.ShapeDrawable
import android.graphics.drawable.StateListDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import kotlinx.android.synthetic.main.fragment_guardian_verify.*
import org.rfcx.audiomoth.R
import org.rfcx.audiomoth.view.deployment.guardian.GuardianDeploymentProtocol

class GuardianVerifyFragment : Fragment() {
    private val listOfSignal by lazy {
        listOf(signalStrength1, signalStrength2, signalStrength3, signalStrength4)
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
        retrieveGuardianSignal()

        finishButton.setOnClickListener {
            deploymentProtocol?.nextStep()
        }
    }

    private fun retrieveGuardianSignal() {
        //TODO: need to be from guardian instead
        val strength = 21 // mock
        when {
            strength > 80 -> {
                showSignalStrength(SignalState.MAX)
                signalDescText.text = getString(R.string.signal_text_4)
            }
            strength > 60 -> {
                showSignalStrength(SignalState.HIGH)
                signalDescText.text = getString(R.string.signal_text_3)
            }
            strength > 40 -> {
                showSignalStrength(SignalState.NORMAL)
                signalDescText.text = getString(R.string.signal_text_2)
            }
            strength > 20 -> {
                showSignalStrength(SignalState.LOW)
                signalDescText.text = getString(R.string.signal_text_1)
            }
            else -> {
                showSignalStrength(SignalState.NONE)
                signalDescText.text = getString(R.string.signal_text_0)
            }
        }
        signalValue.text = strength.toString()
    }

    private fun showSignalStrength(state: SignalState) {
        listOfSignal.forEachIndexed { index, view ->
            if (index < state.value){
                (view.background as GradientDrawable).setBackground(requireContext(), R.color.signal_filled)
            }
        }
    }

    companion object {
        enum class SignalState(val value: Int) {
            NONE(0), LOW(1), NORMAL(2), HIGH(3), MAX(4)
        }

        private fun GradientDrawable.setBackground(context: Context, color: Int) {
            this.setColor(ContextCompat.getColor(context, color))
        }

        fun newInstance(): GuardianVerifyFragment = GuardianVerifyFragment()
    }
}
