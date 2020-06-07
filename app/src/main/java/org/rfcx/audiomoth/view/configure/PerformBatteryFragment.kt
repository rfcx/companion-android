package org.rfcx.audiomoth.view.configure

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
import org.rfcx.audiomoth.entity.Deployment
import org.rfcx.audiomoth.entity.DeploymentState
import org.rfcx.audiomoth.util.NotificationBroadcastReceiver
import org.rfcx.audiomoth.util.toDateTimeString
import org.rfcx.audiomoth.view.deployment.DeploymentProtocol
import java.sql.Timestamp
import java.util.*

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
            val location = deploymentProtocol?.getDeploymentLocation()
            val profileId = deploymentProtocol?.getProfileId()
            if (configuration != null && location != null && profileId != null) {
                val deployment =
                    Deployment(
                        batteryDepletedAt = batteryDepletedAt,
                        deployedAt = deployedAt,
                        batteryLevel = 100,
                        configuration = configuration,
                        location = location,
                        state = DeploymentState.Sync.key
                    )
                notification(batteryDepletedAt, location.name)
//                deploymentProtocol?.saveDeployment(deployment)
            }
        }
    }

    private fun timeFlash() {
        batteryLv1Button.setOnClickListener {
            deploymentProtocol?.openPerformBattery(BATTERY_LEVEL, 1)
        }
        batteryLv2Button.setOnClickListener {
            deploymentProtocol?.openPerformBattery(BATTERY_LEVEL, 2)
        }
        batteryLv3Button.setOnClickListener {
            deploymentProtocol?.openPerformBattery(BATTERY_LEVEL, 3)
        }
        batteryLv4Button.setOnClickListener {
            deploymentProtocol?.openPerformBattery(BATTERY_LEVEL, 4)
        }
        batteryLv5Button.setOnClickListener {
            deploymentProtocol?.openPerformBattery(BATTERY_LEVEL, 5)
        }
    }

    private fun knowBatteryLevel() {
        var level = 0
        var days = ""
        var numberOfDays = 0
        var batteryLevel = 0
        var percent = ""
        arguments?.let {
            level = it.getInt(LEVEL)
        }

        when (level) {
            5 -> {
                numberOfDays = 0
                batteryLevel = 20
                days = getString(R.string.day, "<1")
                percent = getString(R.string.charged, "20%")
            }
            4 -> {
                numberOfDays = 1
                batteryLevel = 40
                days = getString(R.string.day, "1")
                percent = getString(R.string.charged, "40%")
            }
            3 -> {
                numberOfDays = 2
                batteryLevel = 60
                days = getString(R.string.days, "2")
                percent = getString(R.string.charged, "60%")
            }
            2 -> {
                numberOfDays = 4
                batteryLevel = 80
                days = getString(R.string.days, "4")
                percent = getString(R.string.charged, "80%")
            }
            1 -> {
                numberOfDays = 6
                batteryLevel = 100
                days = getString(R.string.days, "6")
                percent = getString(R.string.charged, "100%")
            }
        }

        setBatteryView(level)
        daysTextView.text = days
        chargedTextView.text = percent

        nextButton.setOnClickListener {
            val batteryDepletedAt = Timestamp(System.currentTimeMillis() + (day * numberOfDays))
            val deployedAt = Timestamp(System.currentTimeMillis())
            val configuration = deploymentProtocol?.geConfiguration()
            val location = deploymentProtocol?.getDeploymentLocation()
            val profileId = deploymentProtocol?.getProfileId()
            if (configuration != null && location != null && profileId != null) {
                val deployment =
                Deployment(
                    batteryDepletedAt = batteryDepletedAt,
                    deployedAt = deployedAt,
                    batteryLevel = batteryLevel,
                    configuration = configuration,
                    location = location,
                    state = DeploymentState.Sync.key
                )
                notification(batteryDepletedAt, location.name)
//                deploymentProtocol?.saveDeployment(deployment)
            }
        }
    }

    private fun setBatteryView(level: Int) {
        when (level) {
            5 -> {
                batteryLevel1View.visibility = View.VISIBLE
                batteryLevel2View.visibility = View.INVISIBLE
                batteryLevel3View.visibility = View.INVISIBLE
                batteryLevel4View.visibility = View.INVISIBLE
                batteryLevel5View.visibility = View.INVISIBLE
            }
            4 -> {
                batteryLevel1View.visibility = View.VISIBLE
                batteryLevel2View.visibility = View.VISIBLE
                batteryLevel3View.visibility = View.INVISIBLE
                batteryLevel4View.visibility = View.INVISIBLE
                batteryLevel5View.visibility = View.INVISIBLE
            }
            3 -> {
                batteryLevel1View.visibility = View.VISIBLE
                batteryLevel2View.visibility = View.VISIBLE
                batteryLevel3View.visibility = View.VISIBLE
                batteryLevel4View.visibility = View.INVISIBLE
                batteryLevel5View.visibility = View.INVISIBLE
            }
            2 -> {
                batteryLevel1View.visibility = View.VISIBLE
                batteryLevel2View.visibility = View.VISIBLE
                batteryLevel3View.visibility = View.VISIBLE
                batteryLevel4View.visibility = View.VISIBLE
                batteryLevel5View.visibility = View.INVISIBLE
            }
            1 -> {
                batteryLevel1View.visibility = View.VISIBLE
                batteryLevel2View.visibility = View.VISIBLE
                batteryLevel3View.visibility = View.VISIBLE
                batteryLevel4View.visibility = View.VISIBLE
                batteryLevel5View.visibility = View.VISIBLE
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
        fun newInstance(page: String, level: Int?) = PerformBatteryFragment().apply {
            arguments = Bundle().apply {
                putString(STATUS, page)
                if (level != null) {
                    putInt(LEVEL, level)
                }
            }
        }
    }
}
