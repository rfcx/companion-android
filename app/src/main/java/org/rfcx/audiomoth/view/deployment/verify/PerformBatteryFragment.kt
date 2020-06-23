package org.rfcx.audiomoth.view.deployment.verify

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import kotlinx.android.synthetic.main.confirm_perform_battery.*
import kotlinx.android.synthetic.main.fragment_battery_level.*
import kotlinx.android.synthetic.main.fragment_perform_battery.*
import org.rfcx.audiomoth.R
import org.rfcx.audiomoth.entity.DeploymentLocation
import org.rfcx.audiomoth.util.NotificationBroadcastReceiver
import org.rfcx.audiomoth.util.toDateTimeString
import org.rfcx.audiomoth.view.deployment.DeploymentProtocol
import java.sql.Timestamp
import java.util.*

class PerformBatteryFragment : Fragment() {
    private var status: String? = null
    private var deploymentProtocol: DeploymentProtocol? = null
    private var location: DeploymentLocation? = null
    private val day = 24 * 60 * 60 * 1000

    override fun onAttach(context: Context) {
        super.onAttach(context)
        this.deploymentProtocol = (context as DeploymentProtocol)
        this.location = deploymentProtocol?.getDeploymentLocation()
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
        deploymentProtocol?.hideCompleteButton()
        when (status) {
            TEST_BATTERY -> checkBattery()
            TIME_LED_FLASH -> timeFlash()
            BATTERY_LEVEL -> knowBatteryLevel()
        }
    }

    private fun checkBattery() {
        testButton.setOnClickListener {
            deploymentProtocol?.playCheckBatterySound()
            deploymentProtocol?.startCheckBattery(TIME_LED_FLASH, null)
        }

        skipButton.setOnClickListener {
            val batteryDepletedAt = Timestamp(System.currentTimeMillis() + (day * 6))
            if (location != null) {
                notification(batteryDepletedAt, location!!.name)
                deploymentProtocol?.setPerformBattery(batteryDepletedAt, 100)
                deploymentProtocol?.nextStep()
            }
        }
    }

    private fun timeFlash() {
        tryAgainButton.setOnClickListener {
            deploymentProtocol?.playCheckBatterySound()
        }
        batteryLv1Button.setOnClickListener {
            deploymentProtocol?.startCheckBattery(BATTERY_LEVEL, 1)
        }
        batteryLv2Button.setOnClickListener {
            deploymentProtocol?.startCheckBattery(BATTERY_LEVEL, 2)
        }
        batteryLv3Button.setOnClickListener {
            deploymentProtocol?.startCheckBattery(BATTERY_LEVEL, 3)
        }
        batteryLv4Button.setOnClickListener {
            deploymentProtocol?.startCheckBattery(BATTERY_LEVEL, 4)
        }
        batteryLv5Button.setOnClickListener {
            deploymentProtocol?.startCheckBattery(BATTERY_LEVEL, 5)
        }
        batteryLv6Button.setOnClickListener {
            deploymentProtocol?.startCheckBattery(BATTERY_LEVEL, 6)
        }
    }

    private fun knowBatteryLevel() {
        var level = 0
        var days = ""
        var numberOfDays = 0
        var batteryLevel = 0
        arguments?.let {
            level = it.getInt(LEVEL)
        }

        when (level) {
            1 -> {
                numberOfDays = 1
                batteryLevel = 15
                days = getString(R.string.day, "1")
            }
            2 -> {
                numberOfDays = 2
                batteryLevel = 30
                days = getString(R.string.day, "2")
            }
            3 -> {
                numberOfDays = 3
                batteryLevel = 45
                days = getString(R.string.days, "3")
            }
            4 -> {
                numberOfDays = 4
                batteryLevel = 60
                days = getString(R.string.days, "4")
            }
            5 -> {
                numberOfDays = 5
                batteryLevel = 75
                days = getString(R.string.days, "5")
            }
            6 -> {
                numberOfDays = 6
                batteryLevel = 100
                days = getString(R.string.days, "6")
            }
        }

        setBatteryView(level)
        daysTextView.text = days

        nextButton.setOnClickListener {
            val batteryDepletedAt = Timestamp(System.currentTimeMillis() + (day * numberOfDays))
            if (location != null) {

                notification(batteryDepletedAt, location!!.name)
                deploymentProtocol?.setPerformBattery(batteryDepletedAt, batteryLevel)
                deploymentProtocol?.nextStep()
            }
        }
    }

    private fun setBatteryView(level: Int) {
        when (level) {
            1 -> {
                batteryLevel1View.visibility = View.VISIBLE
                batteryLevel2View.visibility = View.INVISIBLE
                batteryLevel3View.visibility = View.INVISIBLE
                batteryLevel4View.visibility = View.INVISIBLE
                batteryLevel5View.visibility = View.INVISIBLE
                batteryLevel6View.visibility = View.INVISIBLE
            }
            2 -> {
                batteryLevel1View.visibility = View.VISIBLE
                batteryLevel2View.visibility = View.VISIBLE
                batteryLevel3View.visibility = View.INVISIBLE
                batteryLevel4View.visibility = View.INVISIBLE
                batteryLevel5View.visibility = View.INVISIBLE
                batteryLevel6View.visibility = View.INVISIBLE
            }
            3 -> {
                batteryLevel1View.visibility = View.VISIBLE
                batteryLevel2View.visibility = View.VISIBLE
                batteryLevel3View.visibility = View.VISIBLE
                batteryLevel4View.visibility = View.INVISIBLE
                batteryLevel5View.visibility = View.INVISIBLE
                batteryLevel6View.visibility = View.INVISIBLE
            }
            4 -> {
                batteryLevel1View.visibility = View.VISIBLE
                batteryLevel2View.visibility = View.VISIBLE
                batteryLevel3View.visibility = View.VISIBLE
                batteryLevel4View.visibility = View.VISIBLE
                batteryLevel5View.visibility = View.INVISIBLE
                batteryLevel6View.visibility = View.INVISIBLE
            }
            5 -> {
                batteryLevel1View.visibility = View.VISIBLE
                batteryLevel2View.visibility = View.VISIBLE
                batteryLevel3View.visibility = View.VISIBLE
                batteryLevel4View.visibility = View.VISIBLE
                batteryLevel5View.visibility = View.VISIBLE
                batteryLevel6View.visibility = View.INVISIBLE
            }
            6-> {
                batteryLevel1View.visibility = View.VISIBLE
                batteryLevel2View.visibility = View.VISIBLE
                batteryLevel3View.visibility = View.VISIBLE
                batteryLevel4View.visibility = View.VISIBLE
                batteryLevel5View.visibility = View.VISIBLE
                batteryLevel6View.visibility = View.VISIBLE
            }
        }
    }

    private fun notification(batteryDepletedAt: Timestamp, locationName: String) {
        val intent = Intent(context, NotificationBroadcastReceiver::class.java)
        val date = Date(batteryDepletedAt.time)
        val dateAlarm = Date(batteryDepletedAt.time - day)
        intent.putExtra(BATTERY_DEPLETED_AT, date.toDateTimeString())
        intent.putExtra(LOCATION_NAME, locationName)

        val pendingIntent =
            PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)

        val cal = Calendar.getInstance()
        cal.time = dateAlarm

        val alarmManager = context?.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.setExact(
            AlarmManager.RTC_WAKEUP,
            cal.timeInMillis,
            pendingIntent
        )
    }

    companion object {
        const val TAG = "PerformBatteryFragment"
        const val STATUS = "STATUS"
        const val LEVEL = "LEVEL"
        const val TEST_BATTERY = "TEST_BATTERY"
        const val TIME_LED_FLASH = "TIME_LED_FLASH"
        const val BATTERY_LEVEL = "BATTERY_LEVEL"
        const val BATTERY_DEPLETED_AT = "BATTERY_DEPLETED_AT"
        const val LOCATION_NAME = "LOCATION_NAME"

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
