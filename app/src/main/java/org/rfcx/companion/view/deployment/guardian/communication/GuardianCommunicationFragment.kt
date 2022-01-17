package org.rfcx.companion.view.deployment.guardian.communication

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import kotlinx.android.synthetic.main.fragment_guardian_communication_configuration.*
import org.rfcx.companion.R
import org.rfcx.companion.connection.socket.AdminSocketManager
import org.rfcx.companion.connection.socket.GuardianSocketManager
import org.rfcx.companion.view.deployment.guardian.GuardianDeploymentProtocol

class GuardianCommunicationFragment : Fragment() {

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
        return inflater.inflate(R.layout.fragment_guardian_communication_configuration, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        deploymentProtocol?.let {
            it.setToolbarSubtitle()
            it.setMenuToolbar(false)
            it.showToolbar()
            it.setToolbarTitle()
        }

        setSimDetected()
        setPhoneNumber()
        setSatDetected()

        setPlanRadioGroup()

        nextButton.setOnClickListener {
            handlePlanSelection()
            deploymentProtocol?.nextStep()
        }
    }

    private fun setSimDetected() {
        AdminSocketManager.pingBlob.observe(viewLifecycleOwner, Observer {
            val isSimDetected = deploymentProtocol?.getSimDetected()

            simDetectionCheckbox.isChecked = !(isSimDetected == null || isSimDetected == false)
        })
    }

    private fun setPhoneNumber() {
        AdminSocketManager.pingBlob.observe(viewLifecycleOwner, Observer {
            val phoneNumber = deploymentProtocol?.getPhoneNumber()
            if (phoneNumber != null) {
                phoneNumberValueTextView.text = phoneNumber
            }
        })
    }

    private fun setSatDetected() {
        AdminSocketManager.pingBlob.observe(viewLifecycleOwner, Observer {
            val satId = deploymentProtocol?.getSatId()
            if (satId == null) {
                satDetectionCheckbox.isChecked = false
                swarmValueTextView.visibility = View.GONE
            } else {
                satDetectionCheckbox.isChecked = true
                swarmValueTextView.visibility = View.VISIBLE
                swarmValueTextView.text = satId
            }
        })
    }

    private fun setPlanRadioGroup() {
        guardianPlanGroup.setOnCheckedChangeListener { group, checkedId ->
            if (checkedId == R.id.satOnlyRadioButton) {
                passTimeEditText.isEnabled = true
            }
        }
    }

    private fun handlePlanSelection() {
        if (cellOnlyRadioButton.isChecked) {
            GuardianSocketManager.sendCellOnlyPrefs()
        }
        if (cellSmsRadioButton.isChecked) {
            GuardianSocketManager.sendCellSMSPrefs()
        }
        if (satOnlyRadioButton.isChecked) {
            GuardianSocketManager.sendSatOnlyPrefs()
        }
    }

    companion object {
        fun newInstance(): GuardianCommunicationFragment = GuardianCommunicationFragment()
    }
}
