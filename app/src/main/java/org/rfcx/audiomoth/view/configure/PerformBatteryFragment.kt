package org.rfcx.audiomoth.view.configure

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import kotlinx.android.synthetic.main.confirm_perform_battery.*
import kotlinx.android.synthetic.main.fragment_battery_level.*
import kotlinx.android.synthetic.main.fragment_perform_battery.*
import org.rfcx.audiomoth.R
import org.rfcx.audiomoth.entity.Deployment
import org.rfcx.audiomoth.view.DeploymentProtocol
import java.sql.Timestamp

class PerformBatteryFragment : Fragment() {
    private var status: String? = null
    private var deploymentProtocol: DeploymentProtocol? = null
    private val day = 24 * 60 * 60 * 1000

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
                inflater.inflate(R.layout.fragment_battery_level, container, false)
            BATTERY_LEVEL -> view =
                inflater.inflate(R.layout.confirm_perform_battery, container, false)
        }
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        when (status) {
            TEST_BATTERY -> checkBattery()
            TIME_LED_FLASH -> timeFlash()
            BATTERY_LEVEL -> knowBatteryLevel()
        }
    }

    private fun checkBattery() {
        testButton.setOnClickListener {
            deploymentProtocol?.openPerformBattery(TIME_LED_FLASH, null)
        }

        skipButton.setOnClickListener {
            val batteryDepletedAt = Timestamp(System.currentTimeMillis() + (day * 6))
            val deployedAt = Timestamp(System.currentTimeMillis())
            val configuration = deploymentProtocol?.geConfiguration()
            val location = deploymentProtocol?.getLocationInDeployment()
            val profileId = deploymentProtocol?.getProfileId()
            if (configuration != null && location != null && profileId != null) {
                val deployment =
                    Deployment(
                        batteryDepletedAt,
                        deployedAt,
                        100,
                        true,
                        configuration,
                        location,
                        profileId
                    )
                deploymentProtocol?.saveDeployment(deployment)
            }
        }
    }

    private fun timeFlash() {
        batteryLv1Button.setOnClickListener {
            deploymentProtocol?.openPerformBattery(
                BATTERY_LEVEL,
                R.drawable.battery_level_5
            )
        }
        batteryLv2Button.setOnClickListener {
            deploymentProtocol?.openPerformBattery(
                BATTERY_LEVEL,
                R.drawable.battery_level_4
            )
        }
        batteryLv3Button.setOnClickListener {
            deploymentProtocol?.openPerformBattery(
                BATTERY_LEVEL,
                R.drawable.battery_level_3
            )
        }
        batteryLv4Button.setOnClickListener {
            deploymentProtocol?.openPerformBattery(
                BATTERY_LEVEL,
                R.drawable.battery_level_2
            )
        }
        batteryLv5Button.setOnClickListener {
            deploymentProtocol?.openPerformBattery(
                BATTERY_LEVEL,
                R.drawable.battery_level_1
            )
        }
    }

    private fun knowBatteryLevel() {
        var image = 0
        var days = ""
        var numberOfDays = 0
        var batteryLevel = 0
        var percent = ""
        arguments?.let {
            image = it.getInt(IMAGE)
        }

        when (image) {
            R.drawable.battery_level_1 -> {
                numberOfDays = 0
                batteryLevel = 20
                days = getString(R.string.day, "<1")
                percent = getString(R.string.charged, "20%")
            }
            R.drawable.battery_level_2 -> {
                numberOfDays = 1
                batteryLevel = 40
                days = getString(R.string.day, "1")
                percent = getString(R.string.charged, "40%")
            }
            R.drawable.battery_level_3 -> {
                numberOfDays = 2
                batteryLevel = 60
                days = getString(R.string.days, "2")
                percent = getString(R.string.charged, "60%")
            }
            R.drawable.battery_level_4 -> {
                numberOfDays = 4
                batteryLevel = 80
                days = getString(R.string.days, "4")
                percent = getString(R.string.charged, "80%")
            }
            R.drawable.battery_level_5 -> {
                numberOfDays = 6
                batteryLevel = 100
                days = getString(R.string.days, "6")
                percent = getString(R.string.charged, "100%")
            }
        }

        daysTextView.text = days
        chargedTextView.text = percent
        batteryLevelImageView.setImageResource(image)

        nextButton.setOnClickListener {
            val batteryDepletedAt = Timestamp(System.currentTimeMillis() + (day * numberOfDays))
            val deployedAt = Timestamp(System.currentTimeMillis())
            val configuration = deploymentProtocol?.geConfiguration()
            val location = deploymentProtocol?.getLocationInDeployment()
            val profileId = deploymentProtocol?.getProfileId()
            if (configuration != null && location != null && profileId != null) {
                val deployment =
                    Deployment(
                        batteryDepletedAt,
                        deployedAt,
                        batteryLevel,
                        true,
                        configuration,
                        location,
                        profileId
                    )
                deploymentProtocol?.saveDeployment(deployment)
            }
        }
    }

    companion object {
        const val TAG = "PerformBatteryFragment"
        const val STATUS = "STATUS"
        const val IMAGE = "IMAGE"
        const val TEST_BATTERY = "TEST_BATTERY"
        const val TIME_LED_FLASH = "TIME_LED_FLASH"
        const val BATTERY_LEVEL = "BATTERY_LEVEL"

        @JvmStatic
        fun newInstance(page: String, image: Int?) = PerformBatteryFragment().apply {
            arguments = Bundle().apply {
                putString(STATUS, page)
                if (image != null) {
                    putInt(IMAGE, image)
                }
            }
        }
    }
}
