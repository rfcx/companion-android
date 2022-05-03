package org.rfcx.companion.view.deployment.guardian.communication

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.ViewCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import com.google.android.material.chip.Chip
import com.google.android.material.timepicker.MaterialTimePicker
import com.google.android.material.timepicker.TimeFormat
import kotlinx.android.synthetic.main.fragment_guardian_communication_configuration.*
import org.rfcx.companion.R
import org.rfcx.companion.connection.socket.AdminSocketManager
import org.rfcx.companion.connection.socket.GuardianSocketManager
import org.rfcx.companion.util.prefs.GuardianPlan
import org.rfcx.companion.util.toDateTimeString
import org.rfcx.companion.view.deployment.guardian.GuardianDeploymentProtocol
import java.util.*

class GuardianCommunicationFragment : Fragment(), View.OnClickListener {

    private var deploymentProtocol: GuardianDeploymentProtocol? = null

    private var manualTimeOff = arrayListOf<String>()
    private var autoTimeOff = listOf("00:00-01:20", "03:10-08:40", "11:30-13:15", "15:05-20:45", "23:30-23:59")
    private var tempStartHourOff: String? = null
    private var tempEndHourOff: String? = null
    private var isSetFirstGuardianPlan = false
    private var didEditOffTime = false

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
        }

        setSimDetected()
        setPhoneNumber()
        setSatDetected()
        setGPSDetected()
        setGuardianLocalTime()
        setPlanRadioGroup()
        observeOffTime()
        setOffTime()

        nextButton.setOnClickListener {
            handlePlanSelection()
            deploymentProtocol?.nextStep()
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
                    gpsDetectionCheckbox.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_red_error, 0, 0, 0)
                    gpsDetectionTextView.text = getString(R.string.satellite_gps_not_detected)
                } else {
                    gpsDetectionCheckbox.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_checklist_passed, 0, 0, 0)
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
                        systemTimeCheckbox.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_red_error, 0, 0, 0)
                    } else {
                        systemTimeCheckbox.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_checklist_passed, 0, 0, 0)
                    }
                    guardianTimeValuesTextView.text = Date(time).toDateTimeString()
                } else {
                    systemTimeCheckbox.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_red_error, 0, 0, 0)
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
                        null -> {
                            cellOnlyRadioButton.isChecked = false
                            cellSmsRadioButton.isChecked = false
                            satOnlyRadioButton.isChecked = false
                        }
                    }
                    isSetFirstGuardianPlan = true
                }
            }
        )

        guardianPlanGroup.setOnCheckedChangeListener { group, checkedId ->
            if (checkedId == R.id.satOnlyRadioButton) {
                passTimesTextView.visibility = View.VISIBLE
                showChips(true)
            } else {
                passTimesTextView.visibility = View.GONE
                hideChips(false)
                hideChips(true)
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
            if (manualRadioButton.isChecked) {
                GuardianSocketManager.sendSatOnlyPrefs(manualTimeOff.joinToString(","))
            } else {
                GuardianSocketManager.sendSatOnlyPrefs(autoTimeOff.joinToString(","))
            }
        }
    }

    private fun observeOffTime() {
        // TODO: For preset project off time
        if (deploymentProtocol?.getCurrentProjectId() == "agk3cpurb5wm") {
            autoTimeOff.forEach { time ->
                addChip(time, false)
            }
        }

        GuardianSocketManager.pingBlob.observe(
            viewLifecycleOwner,
            Observer {
                deploymentProtocol?.getSatTimeOff()?.forEach { time ->
                    if (!didEditOffTime) addTimeOff(time)
                }
            }
        )
    }

    private fun setOffTime() {
        timeOffRadioGroup.setOnCheckedChangeListener { group, checkedId ->
            if (checkedId == R.id.manualRadioButton) {
                hideChips(false)
                showChips(true)
                hideEmptyOffTimeText()
            } else {
                hideChips(true)
                showChips(false)
                showEmptyOffTimeText()
            }
        }

        val startPicker = MaterialTimePicker.Builder()
            .setTimeFormat(TimeFormat.CLOCK_24H)
            .setHour(12)
            .setMinute(0)
            .setTitleText(getString(R.string.sat_start_time))
            .build()

        val endPicker = MaterialTimePicker.Builder()
            .setTimeFormat(TimeFormat.CLOCK_24H)
            .setHour(12)
            .setMinute(0)
            .setTitleText(R.string.sat_end_time)
            .build()

        startPicker.addOnPositiveButtonClickListener {
            val hour =
                if (startPicker.hour.toString().length == 1) "0${startPicker.hour}" else startPicker.hour.toString()
            val minute =
                if (startPicker.minute.toString().length == 1) "0${startPicker.minute}" else startPicker.minute.toString()
            tempStartHourOff = "$hour:$minute"
            endPicker.show(requireFragmentManager(), "EndTimeOffPicker")
        }

        endPicker.addOnPositiveButtonClickListener {
            val hour =
                if (endPicker.hour.toString().length == 1) "0${endPicker.hour}" else endPicker.hour.toString()
            val minute =
                if (endPicker.minute.toString().length == 1) "0${endPicker.minute}" else endPicker.minute.toString()
            tempEndHourOff = "$hour:$minute"
            val fullTime = "$tempStartHourOff-$tempEndHourOff"
            addTimeOff(fullTime)
        }

        passTimeAddChip.setOnClickListener {
            startPicker.show(requireFragmentManager(), "StartTimeOffPicker")
        }
    }

    private fun addChip(name: String, isManual: Boolean = true) {
        val chip = Chip(requireContext())
        chip.text = name
        chip.id = ViewCompat.generateViewId()
        if (isManual) {
            chip.isCloseIconVisible = true
            chip.setOnCloseIconClickListener(this)
            manualOffTimeChipGroup.addView(chip)
        } else {
            chip.isCloseIconVisible = false
            autoOffTimeChipGroup.addView(chip)
        }
    }

    private fun hideChips(isManual: Boolean) {
        timeOffRadioGroup.visibility = View.GONE
        if (isManual) {
            manualOffTimeChipGroup.visibility = View.GONE
        } else {
            autoOffTimeChipGroup.visibility = View.GONE
        }
    }

    private fun showChips(isManual: Boolean) {
        timeOffRadioGroup.visibility = View.VISIBLE
        if (isManual) {
            manualOffTimeChipGroup.visibility = View.VISIBLE
        } else {
            autoOffTimeChipGroup.visibility = View.VISIBLE
        }
    }

    private fun addTimeOff(time: String) {
        if (manualTimeOff.contains(time)) return
        manualTimeOff.add(time)
        addChip(time)
    }

    private fun showEmptyOffTimeText() {
        if (deploymentProtocol?.getCurrentProjectId() != "agk3cpurb5wm") emptyOffTimeTextView.visibility = View.VISIBLE
    }

    private fun hideEmptyOffTimeText() {
        emptyOffTimeTextView.visibility = View.GONE
    }

    companion object {
        fun newInstance(): GuardianCommunicationFragment = GuardianCommunicationFragment()
    }

    override fun onClick(v: View?) {
        if (v is Chip) {
            didEditOffTime = true
            val time = v.text
            manualTimeOff.remove(time)
            manualOffTimeChipGroup.removeView(v)
        }
    }
}
