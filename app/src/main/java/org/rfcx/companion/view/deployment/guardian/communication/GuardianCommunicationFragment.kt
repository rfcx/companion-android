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
import org.rfcx.companion.util.prefs.GuardianPlan
import org.rfcx.companion.util.toDateTimeString
import org.rfcx.companion.view.deployment.guardian.GuardianDeploymentProtocol
import java.util.*

class GuardianCommunicationFragment : Fragment() {

    private var deploymentProtocol: GuardianDeploymentProtocol? = null

    private var isSetFirstGuardianPlan = false

    private var needCheckSha1 = false
    private var currentPrefsSha1: String? = null
    private var currentPlan: GuardianPlan? = null
    private var currentOffTimes: String? = null

    override fun onAttach(context: Context) {
        super.onAttach(context)
        deploymentProtocol = (context as GuardianDeploymentProtocol)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(
            R.layout.fragment_guardian_communication_configuration,
            container,
            false
        )
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        deploymentProtocol?.let {
            it.setToolbarSubtitle()
            it.setMenuToolbar(false)
            it.showToolbar()
            it.setToolbarTitle()
            currentPrefsSha1 = it.getPrefsSha1()
            currentPlan = it.getGuardianPlan()
            currentOffTimes = it.getSatTimeOff()
        }

        setSimDetected()
        setPhoneNumber()
        setSatDetected()
        setGPSDetected()
        setGuardianLocalTime()
        setPlanRadioGroup()
        setOffTime()

        nextButton.setOnClickListener {
            nextButton.visibility = View.INVISIBLE
            progressBar.visibility = View.VISIBLE
            handlePlanSelection()
            GuardianSocketManager.pingBlob.observe(
                viewLifecycleOwner,
                Observer {
                    requireActivity().runOnUiThread {
                        if (!needCheckSha1) {
                            deploymentProtocol?.nextStep()
                        }
                        if (currentPrefsSha1 != deploymentProtocol?.getPrefsSha1()) {
                            deploymentProtocol?.nextStep()
                        }
                    }
                }
            )
        }
    }

    private fun setSimDetected() {
        AdminSocketManager.pingBlob.observe(
            viewLifecycleOwner,
            Observer {
                val isSimDetected = deploymentProtocol?.getSimDetected()

                if ((isSimDetected == null || isSimDetected == false)) {
                    simDetectionCheckbox.setCompoundDrawablesWithIntrinsicBounds(
                        R.drawable.ic_red_error,
                        0,
                        0,
                        0
                    )
                    simDetectionTextView.text = getString(R.string.sim_not_detected)
                } else {
                    simDetectionCheckbox.setCompoundDrawablesWithIntrinsicBounds(
                        R.drawable.ic_checklist_passed,
                        0,
                        0,
                        0
                    )
                    simDetectionTextView.text = getString(R.string.sim_detected)
                }
            }
        )
    }

    private fun setPhoneNumber() {
        AdminSocketManager.pingBlob.observe(
            viewLifecycleOwner,
            Observer {
                val phoneNumber = deploymentProtocol?.getPhoneNumber()
                if (phoneNumber != null) {
                    phoneNumberValueTextView.text = phoneNumber
                    phoneNumberTextView.visibility = View.VISIBLE
                    phoneNumberValueTextView.visibility = View.VISIBLE
                } else {
                    phoneNumberTextView.visibility = View.GONE
                    phoneNumberValueTextView.visibility = View.GONE
                }
            }
        )
    }

    private fun setSatDetected() {
        AdminSocketManager.pingBlob.observe(
            viewLifecycleOwner,
            Observer {
                val satId = deploymentProtocol?.getSatId()
                if (satId != null) {
                    satDetectionCheckbox.setCompoundDrawablesWithIntrinsicBounds(
                        R.drawable.ic_checklist_passed,
                        0,
                        0,
                        0
                    )
                    satDetectionTextView.text = getString(R.string.satellite_module_detected)
                    swarmIdTextView.visibility = View.VISIBLE
                    swarmValueTextView.visibility = View.VISIBLE
                    swarmValueTextView.text = satId
                    satOnlyRadioButton.isEnabled = true
                    satOnlyRadioButton.setTextColor(resources.getColor(R.color.text_primary))
                } else {
                    satDetectionCheckbox.setCompoundDrawablesWithIntrinsicBounds(
                        R.drawable.ic_red_error,
                        0,
                        0,
                        0
                    )
                    satDetectionTextView.text = getString(R.string.satellite_module_not_detected)
                    swarmIdTextView.visibility = View.GONE
                    swarmValueTextView.visibility = View.GONE
                    val isSimDetected = deploymentProtocol?.getSimDetected()
                    if (isSimDetected == true) {
                        satOnlyRadioButton.isEnabled = false
                        satOnlyRadioButton.setTextColor(resources.getColor(R.color.text_secondary))
                    } else {
                        satOnlyRadioButton.isEnabled = true
                        satOnlyRadioButton.setTextColor(resources.getColor(R.color.text_primary))
                    }
                }
            }
        )
    }

    private fun setGPSDetected() {
        AdminSocketManager.pingBlob.observe(
            viewLifecycleOwner,
            Observer {
                val isGPSDetected = deploymentProtocol?.getGPSDetected()
                if ((isGPSDetected == null || isGPSDetected == false)) {
                    gpsDetectionCheckbox.setCompoundDrawablesWithIntrinsicBounds(
                        R.drawable.ic_red_error,
                        0,
                        0,
                        0
                    )
                    gpsDetectionTextView.text = getString(R.string.satellite_gps_not_detected)
                } else {
                    gpsDetectionCheckbox.setCompoundDrawablesWithIntrinsicBounds(
                        R.drawable.ic_checklist_passed,
                        0,
                        0,
                        0
                    )
                    gpsDetectionTextView.text = getString(R.string.satellite_gps_detected)
                }
            }
        )
    }

    private fun setGuardianLocalTime() {
        GuardianSocketManager.pingBlob.observe(
            viewLifecycleOwner,
            Observer {
                val time = deploymentProtocol?.getGuardianLocalTime()
                if (time != null) {
                    if ((System.currentTimeMillis() - time) > (1000 * 60 * 60 * 24)) {
                        systemTimeCheckbox.setCompoundDrawablesWithIntrinsicBounds(
                            R.drawable.ic_red_error,
                            0,
                            0,
                            0
                        )
                    } else {
                        systemTimeCheckbox.setCompoundDrawablesWithIntrinsicBounds(
                            R.drawable.ic_checklist_passed,
                            0,
                            0,
                            0
                        )
                    }
                    guardianTimeValuesTextView.text = Date(time).toDateTimeString()
                } else {
                    systemTimeCheckbox.setCompoundDrawablesWithIntrinsicBounds(
                        R.drawable.ic_red_error,
                        0,
                        0,
                        0
                    )
                    guardianTimeValuesTextView.text = getString(R.string.guardian_local_time_null)
                }

                val timezone = deploymentProtocol?.getGuardianTimezone()
                if (timezone != null && time != null) {
                    timezoneValuesTextView.text = timezone
                    guardianTimeValuesTextView.text = Date(time).toDateTimeString(timezone)
                } else {
                    timezoneValuesTextView.text = getString(R.string.guardian_local_timezone_null)
                }
            }
        )
    }

    private fun setPlanRadioGroup() {
        AdminSocketManager.pingBlob.observe(
            viewLifecycleOwner,
            Observer {
                val guardianPlan = deploymentProtocol?.getGuardianPlan()
                if (guardianPlan != null && !isSetFirstGuardianPlan) {
                    when (guardianPlan) {
                        GuardianPlan.CELL_ONLY -> {
                            cellOnlyRadioButton.isChecked = true
                        }
                        GuardianPlan.CELL_SMS -> {
                            cellSmsRadioButton.isChecked = true
                        }
                        GuardianPlan.SAT_ONLY -> {
                            satOnlyRadioButton.isChecked = true
                        }
                        GuardianPlan.OFFLINE_MODE -> {
                            offlineModeRadioButton.isChecked = true
                        }
                        null -> {
                            cellOnlyRadioButton.isChecked = false
                            cellSmsRadioButton.isChecked = false
                            satOnlyRadioButton.isChecked = false
                            offlineModeRadioButton.isChecked = false
                        }
                    }
                    isSetFirstGuardianPlan = true
                }
            }
        )

        guardianPlanGroup.setOnCheckedChangeListener { group, checkedId ->
            if (checkedId == R.id.satOnlyRadioButton) {
                passTimesTextView.visibility = View.VISIBLE
                timeOffRadioGroup.visibility = View.VISIBLE
                offTimeChipGroup.visibility = View.VISIBLE
            } else {
                passTimesTextView.visibility = View.GONE
                timeOffRadioGroup.visibility = View.GONE
                offTimeChipGroup.visibility = View.GONE
            }
        }
    }

    private fun handlePlanSelection() {
        if (cellOnlyRadioButton.isChecked) {
            if (currentPlan != GuardianPlan.CELL_ONLY) needCheckSha1 = true
            GuardianSocketManager.sendCellOnlyPrefs()
        }
        if (cellSmsRadioButton.isChecked) {
            if (currentPlan != GuardianPlan.CELL_SMS) needCheckSha1 = true
            GuardianSocketManager.sendCellSMSPrefs()
        }
        if (satOnlyRadioButton.isChecked) {
            if (currentPlan != GuardianPlan.SAT_ONLY) needCheckSha1 = true
            if (manualRadioButton.isChecked) {
                if (currentOffTimes != offTimeChipGroup.listOfTime.joinToString(",") { it.toStringFormat() }) needCheckSha1 = true
                GuardianSocketManager.sendSatOnlyPrefs(offTimeChipGroup.listOfTime.joinToString(",") { it.toStringFormat() })
            } else {
                val currentProject = deploymentProtocol?.getCurrentProject()
                val offTimes = currentProject?.offTimes
                if (offTimes != null) {
                    if (currentOffTimes != offTimes) needCheckSha1 = true
                    GuardianSocketManager.sendSatOnlyPrefs(offTimes)
                } else {
                    GuardianSocketManager.sendSatOnlyPrefs()
                }
            }
        }
        if (offlineModeRadioButton.isChecked) {
            if (currentPlan != GuardianPlan.OFFLINE_MODE) needCheckSha1 = true
            GuardianSocketManager.sendOfflineModePrefs()
        }
    }

    private fun setOffTime() {
        offTimeChipGroup.fragmentManager = parentFragmentManager
        offTimeChipGroup.setTimes(deploymentProtocol?.getSatTimeOff())
        timeOffRadioGroup.setOnCheckedChangeListener { group, checkedId ->
            if (checkedId == R.id.manualRadioButton) {
                offTimeChipGroup.allowAdd = true
                offTimeChipGroup.setTimes(deploymentProtocol?.getSatTimeOff())
                hideEmptyOffTimeText()
            } else {
                offTimeChipGroup.allowAdd = false
                offTimeChipGroup.setTimes(deploymentProtocol?.getCurrentProject()?.offTimes)
                showEmptyOffTimeText()
            }
        }
    }

    private fun showEmptyOffTimeText() {
        if (deploymentProtocol?.getCurrentProjectId() == null) emptyOffTimeTextView.visibility =
            View.VISIBLE
    }

    private fun hideEmptyOffTimeText() {
        emptyOffTimeTextView.visibility = View.GONE
    }

    companion object {
        fun newInstance(): GuardianCommunicationFragment = GuardianCommunicationFragment()
    }
}
