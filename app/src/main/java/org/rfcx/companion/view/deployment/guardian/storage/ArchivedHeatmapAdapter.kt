package org.rfcx.companion.view.deployment.guardian.storage

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.item_heatmap_xaxis.view.*
import kotlinx.android.synthetic.main.item_heatmap_xyaxis.view.*
import kotlinx.android.synthetic.main.item_heatmap_yaxis.view.*
import org.rfcx.companion.R
import org.rfcx.companion.view.deployment.CheckListAdapter

class ArchivedHeatmapAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        const val NORMAL_CELL = 1
        const val X_AXIS_CELL = 2
        const val Y_AXIS_CELL = 3
        const val XY_AXIS_CELL = 4
    }

    private var data = listOf<HeatmapItem>()

    fun setData(items: List<HeatmapItem>) {
        data = items
        notifyDataSetChanged()
    }

    override fun getItemViewType(position: Int): Int {
        return when (data[position]) {
            is HeatmapItem.XAxis -> X_AXIS_CELL
            is HeatmapItem.YAxis -> Y_AXIS_CELL
            is HeatmapItem.XYAxis -> XY_AXIS_CELL
            else -> NORMAL_CELL
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            X_AXIS_CELL -> XAxisHeatmapViewHolder(
                LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_heatmap_xaxis, parent, false)
            )
            Y_AXIS_CELL -> YAxisHeatmapViewHolder(
                LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_heatmap_yaxis, parent, false)
            )
            XY_AXIS_CELL -> XYAxisHeatmapViewHolder(
                LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_heatmap_xyaxis, parent, false)
            )
            else -> NormalHeatmapViewHolder(
                LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_heatmap_normal, parent, false)
            )
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder.itemViewType) {
            X_AXIS_CELL -> (holder as XAxisHeatmapViewHolder).bind(data[position] as HeatmapItem.XAxis)
            Y_AXIS_CELL -> (holder as YAxisHeatmapViewHolder).bind(data[position] as HeatmapItem.YAxis)
            XY_AXIS_CELL -> (holder as XYAxisHeatmapViewHolder).bind(data[position] as HeatmapItem.XYAxis)
            else -> (holder as NormalHeatmapViewHolder).bind(data[position] as HeatmapItem.Normal)
        }
    }

    override fun getItemCount(): Int = data.size

    inner class NormalHeatmapViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        fun bind(item: HeatmapItem.Normal) {

        }
    }

    inner class XAxisHeatmapViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val labelText = itemView.xLabelTextView
        fun bind(item: HeatmapItem.XAxis) {
            labelText.text = item.label
        }
    }

    inner class YAxisHeatmapViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val labelText = itemView.yLabelTextView
        fun bind(item: HeatmapItem.YAxis) {
            labelText.text = item.label
        }
    }

    inner class XYAxisHeatmapViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val labelLeftText = itemView.yyLabelTextView
        private val labelBottomText = itemView.xxLabelTextView
        fun bind(item: HeatmapItem.XYAxis) {
            labelLeftText.text = item.labelLeft
            labelBottomText.text = item.labelBottom
        }
    }

}

sealed class HeatmapItem {
    data class Normal(val value: Int) : HeatmapItem()
    data class XAxis(val label: String, val value: Int) : HeatmapItem()
    data class YAxis(val label: String, val value: Int) : HeatmapItem()
    data class XYAxis(val labelLeft: String, val labelBottom: String, val value: Int) :
        HeatmapItem()
}
