package org.rfcx.audiomoth.view.deployment.guardian.solarpanel

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
import org.rfcx.audiomoth.R
import org.rfcx.audiomoth.connection.socket.SocketManager
import org.rfcx.audiomoth.view.deployment.guardian.GuardianDeploymentProtocol
import java.util.*
import kotlin.collections.ArrayList


class GuardianSolarPanelFragment : Fragment() {

    private var deploymentProtocol: GuardianDeploymentProtocol? = null

    private var timer: Timer? = null

    private var isGettingSentinel = false

    private lateinit var voltageLineDataSet: LineDataSet
    private lateinit var powerLineDataSet: LineDataSet

    private var voltageValues = arrayListOf<Int>()
    private var powerValues = arrayListOf<Int>()

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

        deploymentProtocol?.hideCompleteButton()

        setFeedbackChart()
        setChartDataSetting()
        getSentinelValue()
    }

    private fun getSentinelValue() {
        isGettingSentinel = true

        // getting sentinel values every second
        timer = Timer()
        timer?.schedule( object : TimerTask(){
            override fun run() {
                SocketManager.getSentinelBoardValue()
            }
        }, DELAY, MILLI_PERIOD)

        SocketManager.sentinel.observe(viewLifecycleOwner, Observer { sentinelResponse ->
            if (sentinelResponse.sentinel.isSolarAttached) {
                hideAssembleWarn()

                val voltage = sentinelResponse.sentinel.voltage
                val current = sentinelResponse.sentinel.voltage
                val power = sentinelResponse.sentinel.power

                //set 3 top value
                setVoltageValue(voltage)
                setCurrentValue(current)
                setPowerValue(power)

                voltageValues.add(voltage)
                powerValues.add(power)

                //update power and voltage to chart
                updateData()
            } else {
                showAssembleWarn()
            }
        })
    }

    private fun setVoltageValue(value: Int) {
        voltageValueTextView.text = value.toString()
    }

    private fun setCurrentValue(value: Int) {
        currentValueTextView.text = value.toString()
    }

    private fun setPowerValue(value: Int) {
        powerValueTextView.text = value.toString()
    }

    private fun convertArrayIntToEntry(array: ArrayList<Int>): ArrayList<Entry> {
        val values = arrayListOf<Entry>()
        array.forEachIndexed { index, value ->
            values.add(Entry(index.toFloat(), value.toFloat()))
        }
        return values
    }

    private fun setFeedbackChart() {
        //setup simple line chart
        feedbackChart.apply {
            setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.white))
            description.isEnabled = false /* description inside chart */
            setTouchEnabled(false)
            isDragEnabled = true
            setScaleEnabled(true)
        }

        //set x axis
        feedbackChart.xAxis.apply {
            axisMaximum = 20f
            axisMinimum = 0f
            axisLineWidth = 2f
            position = XAxis.XAxisPosition.BOTTOM
        }

        //set y axis
        feedbackChart.axisLeft.apply {
            axisMaximum = 200f
            axisMinimum = 0f
            axisLineColor = Color.RED
            axisLineWidth = 2f
        }
        feedbackChart.axisRight.apply {
            axisMaximum = 100f
            axisMinimum = 0f
            axisLineWidth = 2f
            axisLineColor = Color.BLUE
        }
    }

    private fun setChartDataSetting() {
        //set line data set
        voltageLineDataSet = LineDataSet(convertArrayIntToEntry(voltageValues), "Voltage").apply {
            setDrawIcons(false)
            color = Color.RED
            lineWidth = 1f
            setDrawCircles(false)
            setDrawCircleHole(false)
            formLineWidth = 1f
            formLineDashEffect = DashPathEffect(floatArrayOf(10f, 5f), 0f)
            formSize = 15f
            valueTextSize = 0f
            valueTextColor = Color.RED
            enableDashedHighlightLine(10f, 5f, 0f)
        }
        powerLineDataSet = LineDataSet(convertArrayIntToEntry(powerValues), "Power").apply {
            setDrawIcons(false)
            color = Color.BLUE
            lineWidth = 1f
            setDrawCircles(false)
            setDrawCircleHole(false)
            formLineWidth = 1f
            formLineDashEffect = DashPathEffect(floatArrayOf(10f, 5f), 0f)
            formSize = 15f
            valueTextSize = 0f
            valueTextColor = Color.BLUE
            enableDashedHighlightLine(10f, 5f, 0f)
        }

        val dataSets = arrayListOf<ILineDataSet>()
        dataSets.add(voltageLineDataSet)
        dataSets.add(powerLineDataSet)

        val lineData = LineData(dataSets)
        feedbackChart.data = lineData
    }

    private fun updateData() {
        //get voltage data set
        voltageLineDataSet = feedbackChart.data.getDataSetByIndex(0) as LineDataSet
        voltageLineDataSet.values = convertArrayIntToEntry(voltageValues)
        voltageLineDataSet.notifyDataSetChanged()

        //get power data set
        powerLineDataSet = feedbackChart.data.getDataSetByIndex(1) as LineDataSet
        powerLineDataSet.values = convertArrayIntToEntry(powerValues)
        powerLineDataSet.notifyDataSetChanged()

        //notify and re-view
        feedbackChart.data.notifyDataChanged()
        feedbackChart.notifyDataSetChanged()
        feedbackChart.invalidate()
    }

    private fun showAssembleWarn() {
        solarWarnTextView.visibility = View.VISIBLE
    }

    private fun hideAssembleWarn() {
        solarWarnTextView.visibility = View.INVISIBLE
    }

    override fun onDetach() {
        super.onDetach()
        if(isGettingSentinel) {
            timer?.cancel()
            timer = null
        }
    }

    companion object {
        private const val DELAY = 0L
        private const val MILLI_PERIOD = 1000L

        fun newInstance(): GuardianSolarPanelFragment = GuardianSolarPanelFragment()
    }
}
