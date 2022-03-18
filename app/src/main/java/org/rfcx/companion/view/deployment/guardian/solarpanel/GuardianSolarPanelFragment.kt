package org.rfcx.companion.view.deployment.guardian.solarpanel

import android.content.Context
import android.graphics.Color
import android.graphics.DashPathEffect
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet
import kotlinx.android.synthetic.main.fragment_guardian_solar_panel.*
import org.rfcx.companion.R
import org.rfcx.companion.connection.socket.AdminSocketManager
import org.rfcx.companion.entity.Screen
import org.rfcx.companion.entity.socket.response.SentinelInput
import org.rfcx.companion.util.Analytics
import org.rfcx.companion.view.deployment.guardian.GuardianDeploymentProtocol
import java.util.*

class GuardianSolarPanelFragment : Fragment() {

    private var deploymentProtocol: GuardianDeploymentProtocol? = null

    private lateinit var powerLineDataSet: LineDataSet

    private val analytics by lazy { context?.let { Analytics(it) } }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        deploymentProtocol = (context as GuardianDeploymentProtocol)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_guardian_solar_panel, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        deploymentProtocol?.let {
            it.setToolbarSubtitle()
            it.setMenuToolbar(false)
            it.showToolbar()
            it.setToolbarTitle()
        }

        solarFinishButton.setOnClickListener {
            analytics?.trackClickNextEvent(Screen.GUARDIAN_SOLAR_PANEL.id)
            deploymentProtocol?.nextStep()
        }

        setFeedbackChart()
        setChartDataSetting()
        getI2CAccessibility()
        getSentinelValue()
    }

    private fun getI2CAccessibility() {
        AdminSocketManager.pingBlob.observe(
            viewLifecycleOwner,
            Observer {
                val i2CAccessibility = deploymentProtocol?.getI2cAccessibility()
                i2CAccessibility?.let {
                    if (it.isAccessible) {
                        i2cCheckbox.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_checklist_passed, 0, 0, 0)
                        i2cCheckTextView.text = getString(R.string.sentinel_module_detect)
                        i2cFailMessage.visibility = View.GONE
                    } else {
                        i2cCheckbox.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_red_error, 0, 0, 0)
                        i2cCheckTextView.text = getString(R.string.sentinel_module_not_detect)
                        i2cFailMessage.text = it.message
                        i2cFailMessage.visibility = View.VISIBLE
                    }
                }
            }
        )
    }

    private fun getSentinelValue() {
        AdminSocketManager.pingBlob.observe(
            viewLifecycleOwner,
            Observer {
                val sentinelResponse = deploymentProtocol?.getSentinelPower()
                sentinelResponse?.let {
                    val input = sentinelResponse.input
                    if (isSentinelConnected(input)) {
                        hideAssembleWarn()

                        val voltage = input.voltage.toFloat()
                        val current = input.current.toFloat()
                        val power = input.power.toFloat()
                        val mainBattery = sentinelResponse.battery.percentage
                        val internalBattery = deploymentProtocol?.getInternalBattery() ?: -1

                        // set 4 top value
                        setVoltageValue(voltage)
                        setCurrentValue(current)
                        setPowerValue(power)
                        setMainBatteryPercentage(mainBattery)
                        setInternalBatteryPercentage(internalBattery)

                        // update power and voltage to chart
                        updateData(power)

                        // expand xAxis line
                        expandXAxisLine()

                        solarFinishButton.isEnabled = true
                    } else {
                        showAssembleWarn()
                    }
                }
            }
        )
    }

    private fun isSentinelConnected(input: SentinelInput): Boolean {
        return input.voltage != 0 || input.current != 0 || input.power != 0
    }

    private fun setVoltageValue(value: Float) {
        voltageValueTextView.text = "${value}mV"
    }

    private fun setCurrentValue(value: Float) {
        currentValueTextView.text = "${value}mA"
    }

    private fun setPowerValue(value: Float) {
        powerValueTextView.text = "${value}mW"
    }

    private fun setMainBatteryPercentage(value: Double) {
        mainBatteryValueTextView.text = "$value%"
    }

    private fun setInternalBatteryPercentage(value: Int) {
        internalBatteryValueTextView.text = "$value%"
    }

    private fun convertPowerToEntry(power: Float): Entry {
        val powerDataSet = feedbackChart.data.getDataSetByIndex(0) as LineDataSet
        return Entry((powerDataSet.entryCount - 1).toFloat(), power)
    }

    private fun setFeedbackChart() {
        // setup simple line chart
        feedbackChart.apply {
            legend.textColor = ContextCompat.getColor(requireContext(), R.color.text_primary)
            setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.backgroundColor))
            description.isEnabled = false /* description inside chart */
        }

        // set x axis
        feedbackChart.xAxis.apply {
            axisMaximum = X_AXIS_MAXIMUM
            axisMinimum = AXIS_MINIMUM
            axisLineWidth = AXIS_LINE_WIDTH
            position = XAxis.XAxisPosition.BOTTOM
            textColor = ContextCompat.getColor(requireContext(), R.color.text_primary)
        }

        // set y axis
        feedbackChart.axisLeft.apply {
            axisMaximum = LEFT_AXIS_MAXIMUM
            axisMinimum = AXIS_MINIMUM
            axisLineColor = Color.RED
            axisLineWidth = AXIS_LINE_WIDTH
            textColor = ContextCompat.getColor(requireContext(), R.color.text_primary)
        }
    }

    private fun setChartDataSetting() {
        // set line data set
        powerLineDataSet = LineDataSet(arrayListOf<Entry>(), "Power").apply {
            setDrawIcons(false)
            color = Color.RED
            lineWidth = CHART_LINE_WIDTH
            setDrawCircles(false)
            setDrawCircleHole(false)
            formLineWidth = CHART_LINE_WIDTH
            formLineDashEffect = DashPathEffect(floatArrayOf(10f, 5f), 0f)
            formSize = FORM_SIZE
            valueTextSize = CHART_TEXT_SIZE
            enableDashedHighlightLine(10f, 5f, 0f)
        }

        val dataSets = arrayListOf<ILineDataSet>()
        dataSets.add(powerLineDataSet)

        val lineData = LineData(dataSets)
        feedbackChart.data = lineData
    }

    private fun updateData(power: Float) {
        val powerEntry = convertPowerToEntry(power)

        // get power data set
        powerLineDataSet = feedbackChart.data.getDataSetByIndex(0) as LineDataSet
        powerLineDataSet.addEntry(powerEntry)

        // notify and re-view
        feedbackChart.notifyDataSetChanged()
        feedbackChart.invalidate()
    }

    private fun expandXAxisLine() {
        val powerDataSet = feedbackChart.data.getDataSetByIndex(0) as LineDataSet
        if (powerDataSet.entryCount > X_AXIS_MAXIMUM) {
            feedbackChart.xAxis.axisMaximum = powerDataSet.entryCount.toFloat()
            feedbackChart.invalidate()
        }
    }

    private fun showAssembleWarn() {
        solarWarnTextView.visibility = View.VISIBLE
    }

    private fun hideAssembleWarn() {
        solarWarnTextView.visibility = View.INVISIBLE
    }

    override fun onResume() {
        super.onResume()
        analytics?.trackScreen(Screen.GUARDIAN_SOLAR_PANEL)
    }

    companion object {
        private const val X_AXIS_MAXIMUM = 100f
        private const val LEFT_AXIS_MAXIMUM = 15000f
        private const val AXIS_MINIMUM = 0f
        private const val AXIS_LINE_WIDTH = 2f

        private const val CHART_LINE_WIDTH = 1f
        private const val FORM_SIZE = 15f
        private const val CHART_TEXT_SIZE = 0f

        fun newInstance(): GuardianSolarPanelFragment = GuardianSolarPanelFragment()
    }
}
