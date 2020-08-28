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
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet
import kotlinx.android.synthetic.main.fragment_guardian_solar_panel.*
import org.rfcx.audiomoth.R
import org.rfcx.audiomoth.view.deployment.guardian.GuardianDeploymentProtocol


class GuardianSolarPanelFragment : Fragment() {

    private var deploymentProtocol: GuardianDeploymentProtocol? = null

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

        setVoltageValue()
        setCurrentValue()
        setPowerValue()

        setFeedbackChart()
    }

    private fun setVoltageValue() {
        voltageValueTextView.text = "000"
    }

    private fun setCurrentValue() {
        currentValueTextView.text = "000"
    }

    private fun setPowerValue() {
        powerValueTextView.text = "000"
    }

    private fun setFeedbackChart() {
        //setup simple line chart
        feedbackChart.apply {
            setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.white))
            description.isEnabled = false
            setTouchEnabled(false)
            isDragEnabled = true
            setScaleEnabled(true)
        }

        //set x axis
        val xAxis = feedbackChart.xAxis.apply {
            axisMaximum = 20f
            axisMinimum = 0f
            position = XAxis.XAxisPosition.BOTTOM
        }

        //set y axis
        val yAxisLeft = feedbackChart.axisLeft.apply {
            axisMaximum = 200f
            axisMinimum = 0f
        }
        val yAxisRight = feedbackChart.axisRight.apply {
            axisMaximum = 200f
            axisMinimum = 0f
        }

        //set data
        var values = random()
        var set1 = LineDataSet(values, "Data1").apply {
            setDrawIcons(false)
            color = Color.RED
            setCircleColor(Color.RED)
            lineWidth = 1f
            circleRadius = 3f
            setDrawCircleHole(false)
            formLineWidth = 1f
            formLineDashEffect = DashPathEffect(floatArrayOf(10f, 5f), 0f)
            formSize = 15f
            valueTextSize = 9f
            valueTextColor = Color.RED
            enableDashedHighlightLine(10f, 5f, 0f)
        }
        values = random()
        var set2 = LineDataSet(values, "Data2").apply {
            setDrawIcons(false)
            color = Color.YELLOW
            setCircleColor(Color.YELLOW)
            lineWidth = 1f
            circleRadius = 3f
            setDrawCircleHole(false)
            formLineWidth = 1f
            formLineDashEffect = DashPathEffect(floatArrayOf(10f, 5f), 0f)
            formSize = 15f
            valueTextSize = 9f
            valueTextColor = Color.YELLOW
            enableDashedHighlightLine(10f, 5f, 0f)
        }

        val dataSets = arrayListOf<ILineDataSet>()
        dataSets.add(set1)
        dataSets.add(set2)

        val lineData = LineData(dataSets)
        feedbackChart.data = lineData

        solarFinishButton.setOnClickListener {
            set1 = feedbackChart.data.getDataSetByIndex(0) as LineDataSet
            set1.values = random()
            set1.notifyDataSetChanged()
            set2 = feedbackChart.data.getDataSetByIndex(1) as LineDataSet
            set2.values = random()
            set2.notifyDataSetChanged()
            feedbackChart.data.notifyDataChanged()
            feedbackChart.notifyDataSetChanged()
            feedbackChart.invalidate()
        }
    }

    private fun random(): ArrayList<Entry> {
        val values = arrayListOf<Entry>()
        for (i in 0 until 20) {
            val value = ((Math.random() * 100)).toFloat()
            values.add(Entry(i.toFloat(), value))
        }
        return values
    }

    companion object {
        fun newInstance(): GuardianSolarPanelFragment = GuardianSolarPanelFragment()
    }
}
