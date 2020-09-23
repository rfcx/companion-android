package org.rfcx.audiomoth.view.deployment.verify

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import kotlinx.android.synthetic.main.fragment_perform_battery.*
import kotlinx.android.synthetic.main.fragment_perform_battery_level.*
import kotlinx.android.synthetic.main.fragment_select_battery_level.*
import org.rfcx.audiomoth.R
import org.rfcx.audiomoth.entity.DeploymentLocation
import org.rfcx.audiomoth.entity.EdgeBatteryInfo
import org.rfcx.audiomoth.view.deployment.EdgeDeploymentProtocol
import java.sql.Timestamp

class PerformBatteryFragment : Fragment() {
    private var status: String? = null
    private var edgeDeploymentProtocol: EdgeDeploymentProtocol? = null
    private var location: DeploymentLocation? = null
    private val day = 24 * 60 * 60 * 1000

    override fun onAttach(context: Context) {
        super.onAttach(context)
        this.edgeDeploymentProtocol = (context as EdgeDeploymentProtocol)
        this.location = edgeDeploymentProtocol?.getDeploymentLocation()
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
                inflater.inflate(R.layout.fragment_select_battery_level, container, false)
            BATTERY_LEVEL -> view =
                inflater.inflate(R.layout.fragment_perform_battery_level, container, false)
        }
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        edgeDeploymentProtocol?.let {
            it.showToolbar()
            it.setToolbarTitle()
        }

        when (status) {
            TEST_BATTERY -> checkBattery()
            TIME_LED_FLASH -> timeFlash()
            BATTERY_LEVEL -> knowBatteryLevel()
        }
    }

    private fun checkBattery() {
        testButton.setOnClickListener {
            edgeDeploymentProtocol?.playCheckBatterySound()
            edgeDeploymentProtocol?.startCheckBattery(TIME_LED_FLASH, null)
        }

        skipButton.setOnClickListener {
            val batteryDepletedAt = Timestamp(System.currentTimeMillis() + (day * 8))
            if (location != null) {
                edgeDeploymentProtocol?.setPerformBattery(batteryDepletedAt, 100)
                edgeDeploymentProtocol?.nextStep()
            }
        }
    }

    private fun timeFlash() {
        tryAgainButton.setOnClickListener {
            edgeDeploymentProtocol?.playCheckBatterySound()
        }
        batteryLv1Button.setOnClickListener {
            edgeDeploymentProtocol?.startCheckBattery(BATTERY_LEVEL, 8)
        }
        batteryLv2Button.setOnClickListener {
            edgeDeploymentProtocol?.startCheckBattery(BATTERY_LEVEL, 7)
        }
        batteryLv3Button.setOnClickListener {
            edgeDeploymentProtocol?.startCheckBattery(BATTERY_LEVEL, 6)
        }
        batteryLv4Button.setOnClickListener {
            edgeDeploymentProtocol?.startCheckBattery(BATTERY_LEVEL, 5)
        }
        batteryLv5Button.setOnClickListener {
            edgeDeploymentProtocol?.startCheckBattery(BATTERY_LEVEL, 4)
        }
        batteryLv6Button.setOnClickListener {
            edgeDeploymentProtocol?.startCheckBattery(BATTERY_LEVEL, 3)
        }
        batteryLv7Button.setOnClickListener {
            edgeDeploymentProtocol?.startCheckBattery(BATTERY_LEVEL, 2)
        }
        batteryLv8Button.setOnClickListener {
            edgeDeploymentProtocol?.startCheckBattery(BATTERY_LEVEL, 1)
        }
        batteryLv9Button.setOnClickListener {
            edgeDeploymentProtocol?.startCheckBattery(BATTERY_LEVEL, 0)
        }
    }

    private fun knowBatteryLevel() {
        var level = 0
        arguments?.let {
            level = it.getInt(LEVEL)
        }
        val batteryDetail = setData(level)
        setBatteryView(level)
        daysTextView.text = if (level == 0) getString(R.string.recharging) else batteryDetail.days
        chargedTextView.text =
            when (level) {
                0 -> getString(R.string.too_low_battery)
                1 -> getString(R.string.recharge_or_replace)
                else -> getString(
                    R.string.notification
                )
            }
        nextButton.setOnClickListener {
            val batteryDepletedAt =
                Timestamp(System.currentTimeMillis() + (day * batteryDetail.numberOfDays))
            if (location != null) {
                edgeDeploymentProtocol?.setPerformBattery(
                    batteryDepletedAt,
                    batteryDetail.batteryLevel
                )
                edgeDeploymentProtocol?.nextStep()
            }
        }
    }

    private fun setData(level: Int): EdgeBatteryInfo {
        return EdgeBatteryInfo(
            days = getString(
                if (level == 1) R.string.day else R.string.days,
                if (level == 0) ">1" else level.toString()
            ),
            numberOfDays = level,
            batteryLevel = if (level == 8) 100 else (100 / 8) * level
        )
    }

    private fun setBatteryView(level: Int) {
        when (level) {
            0 -> {
                batteryLevelLowView.visibility = View.VISIBLE
                setBatteryLevel()
            }
            1 -> setBatteryLevel(view1 = true)
            2 -> setBatteryLevel(view1 = true, view2 = true)
            3 -> setBatteryLevel(view1 = true, view2 = true, view3 = true)
            4 -> setBatteryLevel(view1 = true, view2 = true, view3 = true, view4 = true)
            5 -> setBatteryLevel(
                view1 = true,
                view2 = true,
                view3 = true,
                view4 = true,
                view5 = true
            )
            6 -> setBatteryLevel(
                view1 = true,
                view2 = true,
                view3 = true,
                view4 = true,
                view5 = true,
                view6 = true
            )
            7 -> setBatteryLevel(
                view1 = true,
                view2 = true,
                view3 = true,
                view4 = true,
                view5 = true,
                view6 = true,
                view7 = true
            )
            8 -> setBatteryLevel(
                view1 = true,
                view2 = true,
                view3 = true,
                view4 = true,
                view5 = true,
                view6 = true,
                view7 = true,
                view8 = true
            )
        }
    }

    private fun setBatteryLevel(
        view1: Boolean = false,
        view2: Boolean = false,
        view3: Boolean = false,
        view4: Boolean = false,
        view5: Boolean = false,
        view6: Boolean = false,
        view7: Boolean = false,
        view8: Boolean = false
    ) {
        batteryLevel1View.visibility = if (view1) View.VISIBLE else View.INVISIBLE
        batteryLevel2View.visibility = if (view2) View.VISIBLE else View.INVISIBLE
        batteryLevel3View.visibility = if (view3) View.VISIBLE else View.INVISIBLE
        batteryLevel4View.visibility = if (view4) View.VISIBLE else View.INVISIBLE
        batteryLevel5View.visibility = if (view5) View.VISIBLE else View.INVISIBLE
        batteryLevel6View.visibility = if (view6) View.VISIBLE else View.INVISIBLE
        batteryLevel7View.visibility = if (view7) View.VISIBLE else View.INVISIBLE
        batteryLevel8View.visibility = if (view8) View.VISIBLE else View.INVISIBLE
    }

    companion object {
        const val TAG = "PerformBatteryFragment"
        const val STATUS = "STATUS"
        const val LEVEL = "LEVEL"
        const val TEST_BATTERY = "TEST_BATTERY"
        const val TIME_LED_FLASH = "TIME_LED_FLASH"
        const val BATTERY_LEVEL = "BATTERY_LEVEL"

        @JvmStatic
        fun newInstance(page: String, level: Int?) = PerformBatteryFragment()
            .apply {
                arguments = Bundle().apply {
                    putString(STATUS, page)
                    if (level != null) {
                        putInt(LEVEL, level)
                    }
                }
            }
    }
}
