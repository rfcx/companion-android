package org.rfcx.audiomoth.view.deployment.guardian.register

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.observe
import kotlinx.android.synthetic.main.fragment_guardian_register.*
import org.rfcx.audiomoth.R
import org.rfcx.audiomoth.connection.socket.SocketManager
import org.rfcx.audiomoth.entity.socket.response.Status
import org.rfcx.audiomoth.view.deployment.guardian.GuardianDeploymentProtocol

class GuardianRegisterFragment : Fragment() {

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
        return inflater.inflate(R.layout.fragment_guardian_register, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        deploymentProtocol?.hideCompleteButton()

        isGuardianRegistered()

        registerGuardianButton.setOnClickListener {
            registerGuardian()
        }

        registerFinishButton.setOnClickListener {
            deploymentProtocol?.nextStep()
        }
    }

    private fun setUIWaitingRegisterResponse() {
        registerGuardianButton.isEnabled = false
        registerResultTextView.text = requireContext().getString(R.string.registering)
    }

    private fun registerGuardian() {
        SocketManager.sendGuardianRegistration(requireContext(), getRadioValueForRegistration())
        SocketManager.register.observe(viewLifecycleOwner, Observer {
            if (it.register.status == Status.SUCCESS.value) {
                registerFinishButton.visibility = View.VISIBLE
                registerGuardianButton.visibility = View.GONE
                registerResultTextView.text = requireContext().getString(R.string.register_success)
            } else {
                registerGuardianButton.isEnabled = true
                registerResultTextView.text = requireContext().getString(R.string.register_failed)
            }
        })
        setUIWaitingRegisterResponse()
    }

    private fun isGuardianRegistered() {
        SocketManager.isGuardianRegistered()
        SocketManager.isRegistered.observe(viewLifecycleOwner, Observer {
            if (it.isRegistered) {
                productionRadioButton.isEnabled = false
                stagingRadioButton.isEnabled = false
                registerGuardianButton.visibility = View.GONE
                registerResultTextView.text =
                    requireContext().getString(R.string.already_registered)
                registerFinishButton.visibility = View.VISIBLE
            } else {
                registerResultTextView.text = requireContext().getString(R.string.not_registered)
                registerGuardianButton.isEnabled = true
            }
        })
        registerResultTextView.text = requireContext().getString(R.string.check_registered)
        registerGuardianButton.isEnabled = false
    }

    private fun getRadioValueForRegistration(): Boolean {
        return productionRadioButton.isChecked
    }

    companion object {
        fun newInstance(): GuardianRegisterFragment {
            return GuardianRegisterFragment()
        }
    }

}
