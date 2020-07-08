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
import org.rfcx.audiomoth.entity.BatteryDetail
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
            val batteryDepletedAt = Timestamp(System.currentTimeMillis() + (day * 12))
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
            deploymentProtocol?.startCheckBattery(BATTERY_LEVEL, 12)
        }
        batteryLv2Button.setOnClickListener {
            deploymentProtocol?.startCheckBattery(BATTERY_LEVEL, 11)
        }
        batteryLv3Button.setOnClickListener {
            deploymentProtocol?.startCheckBattery(BATTERY_LEVEL, 10)
        }
        batteryLv4Button.setOnClickListener {
            deploymentProtocol?.startCheckBattery(BATTERY_LEVEL, 9)
        }
        batteryLv5Button.setOnClickListener {
            deploymentProtocol?.startCheckBattery(BATTERY_LEVEL, 8)
        }
        batteryLv6Button.setOnClickListener {
            deploymentProtocol?.startCheckBattery(BATTERY_LEVEL, 7)
        }
        batteryLv7Button.setOnClickListener {
            deploymentProtocol?.startCheckBattery(BATTERY_LEVEL, 6)
        }
        batteryLv8Button.setOnClickListener {
            deploymentProtocol?.startCheckBattery(BATTERY_LEVEL, 5)
        }
        batteryLv9Button.setOnClickListener {
            deploymentProtocol?.startCheckBattery(BATTERY_LEVEL, 4)
        }
        batteryLv10Button.setOnClickListener {
            deploymentProtocol?.startCheckBattery(BATTERY_LEVEL, 3)
        }
        batteryLv11Button.setOnClickListener {
            deploymentProtocol?.startCheckBattery(BATTERY_LEVEL, 2)
        }
        batteryLv12Button.setOnClickListener {
            deploymentProtocol?.startCheckBattery(BATTERY_LEVEL, 1)
        }
    }

    private fun knowBatteryLevel() {
        var level = 0
        arguments?.let {
            level = it.getInt(LEVEL)
        }
        val batteryDetail = setData(level)
        setBatteryView(level)
        daysTextView.text = batteryDetail.days

        nextButton.setOnClickListener {
            val batteryDepletedAt =
                Timestamp(System.currentTimeMillis() + (day * batteryDetail.numberOfDays))
            if (location != null) {

                notification(batteryDepletedAt, location!!.name)
                deploymentProtocol?.setPerformBattery(batteryDepletedAt, batteryDetail.batteryLevel)
                deploymentProtocol?.nextStep()
            }
        }
    }

    private fun setData(level: Int): BatteryDetail {
        return BatteryDetail(
            days = getString(
                if (level == 1) R.string.day else R.string.days,
                level.toString()
            ),
            numberOfDays = level,
            batteryLevel = if (level == 12) 100 else (100 / 12) * level
        )
    }

    private fun setBatteryView(level: Int) {
        when (level) {
            1, 2 -> {
                batteryLevel1View.visibility = View.VISIBLE
                batteryLevel2View.visibility = View.INVISIBLE
                batteryLevel3View.visibility = View.INVISIBLE
                batteryLevel4View.visibility = View.INVISIBLE
                batteryLevel5View.visibility = View.INVISIBLE
                batteryLevel6View.visibility = View.INVISIBLE
            }
            3, 4 -> {
                batteryLevel1View.visibility = View.VISIBLE
                batteryLevel2View.visibility = View.VISIBLE
                batteryLevel3View.visibility = View.INVISIBLE
                batteryLevel4View.visibility = View.INVISIBLE
                batteryLevel5View.visibility = View.INVISIBLE
                batteryLevel6View.visibility = View.INVISIBLE
            }
            5, 6 -> {
                batteryLevel1View.visibility = View.VISIBLE
                batteryLevel2View.visibility = View.VISIBLE
                batteryLevel3View.visibility = View.VISIBLE
                batteryLevel4View.visibility = View.INVISIBLE
                batteryLevel5View.visibility = View.INVISIBLE
                batteryLevel6View.visibility = View.INVISIBLE
            }
            7, 8 -> {
                batteryLevel1View.visibility = View.VISIBLE
                batteryLevel2View.visibility = View.VISIBLE
                batteryLevel3View.visibility = View.VISIBLE
                batteryLevel4View.visibility = View.VISIBLE
                batteryLevel5View.visibility = View.INVISIBLE
                batteryLevel6View.visibility = View.INVISIBLE
            }
            9, 10 -> {
                batteryLevel1View.visibility = View.VISIBLE
                batteryLevel2View.visibility = View.VISIBLE
                batteryLevel3View.visibility = View.VISIBLE
                batteryLevel4View.visibility = View.VISIBLE
                batteryLevel5View.visibility = View.VISIBLE
                batteryLevel6View.visibility = View.INVISIBLE
            }
            11, 12 -> {
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
