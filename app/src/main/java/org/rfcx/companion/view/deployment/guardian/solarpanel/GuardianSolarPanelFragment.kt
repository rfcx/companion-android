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
import java.util.*
import kotlinx.android.synthetic.main.fragment_guardian_solar_panel.*
import org.rfcx.companion.R
import org.rfcx.companion.connection.socket.SocketManager
import org.rfcx.companion.entity.Screen
import org.rfcx.companion.entity.socket.response.SentinelInput
import org.rfcx.companion.util.Analytics
import org.rfcx.companion.view.deployment.guardian.GuardianDeploymentProtocol

class GuardianSolarPanelFragment : Fragment() {

    private var deploymentProtocol: GuardianDeploymentProtocol? = null

    private var timer: Timer = Timer()

    private var isGettingSentinel = false

    private lateinit var voltageLineDataSet: LineDataSet
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
            it.showToolbar()
            it.setToolbarTitle()
        }

        solarFinishButton.setOnClickListener {
            analytics?.trackClickNextEvent(Screen.GUARDIAN_SOLAR_PANEL.id)
            deploymentProtocol?.nextStep()
        }

        setFeedbackChart()
        setChartDataSetting()
        getSentinelValue()
    }

    private fun getSentinelValue() {
        isGettingSentinel = true

        // getting sentinel values every second
        timer = Timer()
        timer.schedule(object : TimerTask() {
            override fun run() {
                SocketManager.getSentinelBoardValue()
            }
        }, DELAY, MILLI_PERIOD)

        SocketManager.sentinel.observe(viewLifecycleOwner, Observer { sentinelResponse ->
            val input = sentinelResponse.sentinel.convertToInfo().input
            if (isSentinelConnected(input)) {
                hideAssembleWarn()

                val voltage = input.voltage.toFloat() / 1000
                val current = input.current.toFloat() / 1000
                val power = input.power.toFloat() / 1000

                // set 3 top value
                setVoltageValue(voltage)
                setCurrentValue(current)
                setPowerValue(power)

                // update power and voltage to chart
                updateData(voltage, power)

                // expand xAxis line
                expandXAxisLine()

                solarFinishButton.isEnabled = true
            } else {
                showAssembleWarn()
            }
        })
    }

    private fun isSentinelConnected(input: SentinelInput): Boolean {
        return input.voltage != 0 && input.current != 0 && input.power != 0
    }

    private fun setVoltageValue(value: Float) {
        voltageValueTextView.text = value.toString()
    }

    private fun setCurrentValue(value: Float) {
        currentValueTextView.text = value.toString()
    }

    private fun setPowerValue(value: Float) {
        powerValueTextView.text = value.toString()
    }

    private fun convertVoltageAndPowerToEntry(voltage: Float, power: Float): Pair<Entry, Entry> {
        val voltageDataSet = feedbackChart.data.getDataSetByIndex(0) as LineDataSet
        val powerDataSet = feedbackChart.data.getDataSetByIndex(1) as LineDataSet
        return Pair(
            Entry((voltageDataSet.entryCount - 1).toFloat(), voltage),
            Entry((powerDataSet.entryCount - 1).toFloat(), power)
        )
    }

    private fun setFeedbackChart() {
        // setup simple line chart
        feedbackChart.apply {
            setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.backgroundColor))
            description.isEnabled = false /* description inside chart */
            setTouchEnabled(false)
            isDragEnabled = true
            setScaleEnabled(true)
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
        feedbackChart.axisRight.apply {
            axisMaximum = RIGHT_AXIS_MAXIMUM
            axisMinimum = AXIS_MINIMUM
            axisLineWidth = AXIS_LINE_WIDTH
            axisLineColor = Color.BLUE
            textColor = ContextCompat.getColor(requireContext(), R.color.text_primary)
        }
    }

    private fun setChartDataSetting() {
        // set line data set
        voltageLineDataSet = LineDataSet(arrayListOf<Entry>(), "Voltage").apply {
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
        powerLineDataSet = LineDataSet(arrayListOf<Entry>(), "Power").apply {
            setDrawIcons(false)
            color = Color.BLUE
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
        dataSets.add(voltageLineDataSet)
        dataSets.add(powerLineDataSet)

        val lineData = LineData(dataSets)
        feedbackChart.data = lineData
    }

    private fun updateData(voltage: Float, power: Float) {
        val pair = convertVoltageAndPowerToEntry(voltage, power)
        // get voltage data set
        voltageLineDataSet = feedbackChart.data.getDataSetByIndex(0) as LineDataSet
        voltageLineDataSet.addEntry(pair.first)
        voltageLineDataSet.notifyDataSetChanged()

        // get power data set
        powerLineDataSet = feedbackChart.data.getDataSetByIndex(1) as LineDataSet
        powerLineDataSet.addEntry(pair.second)
        powerLineDataSet.notifyDataSetChanged()

        // notify and re-view
        feedbackChart.data.notifyDataChanged()
        feedbackChart.notifyDataSetChanged()
        feedbackChart.invalidate()
    }

    private fun expandXAxisLine() {
        // both voltage and power will have the same size
        val voltageDataSet = feedbackChart.data.getDataSetByIndex(0) as LineDataSet
        if (voltageDataSet.entryCount > X_AXIS_MAXIMUM) {
            feedbackChart.xAxis.axisMaximum = voltageDataSet.entryCount.toFloat()
            feedbackChart.invalidate()
        }
    }

    private fun showAssembleWarn() {
        solarWarnTextView.visibility = View.VISIBLE
    }

    private fun hideAssembleWarn() {
        solarWarnTextView.visibility = View.INVISIBLE
    }

    override fun onDetach() {
        super.onDetach()
        if (isGettingSentinel) {
            timer.cancel()
        }
    }

    override fun onResume() {
        super.onResume()
        analytics?.trackScreen(Screen.GUARDIAN_SOLAR_PANEL)
    }

    companion object {
        private const val DELAY = 0L
        private const val MILLI_PERIOD = 1000L

        private const val X_AXIS_MAXIMUM = 100f
        private const val LEFT_AXIS_MAXIMUM = 30f
        private const val RIGHT_AXIS_MAXIMUM = 30f
        private const val AXIS_MINIMUM = 0f
        private const val AXIS_LINE_WIDTH = 2f

        private const val CHART_LINE_WIDTH = 1f
        private const val FORM_SIZE = 15f
        private const val CHART_TEXT_SIZE = 0f

        fun newInstance(): GuardianSolarPanelFragment = GuardianSolarPanelFragment()
    }
}
