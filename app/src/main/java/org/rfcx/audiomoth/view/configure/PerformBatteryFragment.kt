package org.rfcx.audiomoth.view.configure

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import kotlinx.android.synthetic.main.fragment_perform_battery.*
import org.rfcx.audiomoth.R
import org.rfcx.audiomoth.view.DeploymentProtocol

class PerformBatteryFragment : Fragment() {
    private var status: String? = null
    private var deploymentProtocol: DeploymentProtocol? = null

    override fun onAttach(context: Context) {
        super.onAttach(context)
        deploymentProtocol = (context as DeploymentProtocol)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        var view = inflater.inflate(R.layout.fragment_perform_battery, container, false)
        arguments?.let { status = it.getString(STATUS) }

        when (status) {
            TIME_LED_FLASH -> view =
                inflater.inflate(R.layout.fragment_battery_level_bottom_sheet, container, false)
            BATTERY_LEVEL -> view =
                inflater.inflate(R.layout.confirm_perform_battery, container, false)
        }
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        when (status) {
            TEST_BATTERY -> {
                testButton.setOnClickListener {
                    deploymentProtocol?.openPerformBattery(TIME_LED_FLASH)
                }

                skipButton.setOnClickListener {
                    deploymentProtocol?.nextStep()
                }
            }
            TIME_LED_FLASH -> {

            }
            BATTERY_LEVEL -> {

            }
        }
    }

    companion object {
        const val TAG = "PerformBatteryFragment"
        const val STATUS = "STATUS"
        const val TEST_BATTERY = "TEST_BATTERY"
        const val TIME_LED_FLASH = "TIME_LED_FLASH"
        const val BATTERY_LEVEL = "BATTERY_LEVEL"

        @JvmStatic
        fun newInstance(page: String) = PerformBatteryFragment().apply {
            arguments = Bundle().apply {
                putString(STATUS, page)
            }
        }
    }
}

//interface BatteryLevelListener {
//    fun onSelectedBatteryLevel(level: Int)
//}
//
//class BatteryLevelBottomSheetFragment(private val listener: BatteryLevelListener) :
//    BottomSheetDialogFragment() {
//    override fun onCreateView(
//        inflater: LayoutInflater, container: ViewGroup?,
//        savedInstanceState: Bundle?
//    ): View? {
//        return inflater.inflate(R.layout.fragment_battery_level_bottom_sheet, container, false)
//    }
//
//    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
//        super.onViewCreated(view, savedInstanceState)
//        setView()
//    }
//
//    private fun setView() {
//        batteryLv1Button.setOnClickListener { listener.onSelectedBatteryLevel(1) }
//        batteryLv2Button.setOnClickListener { listener.onSelectedBatteryLevel(2) }
//        batteryLv3Button.setOnClickListener { listener.onSelectedBatteryLevel(3) }
//    }
//
//    companion object {
//        const val TAG = "BatteryLevelDialog"
//    }
//}