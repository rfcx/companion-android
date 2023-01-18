package org.rfcx.companion.view.deployment.guardian.storage

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.item_heatmap_normal.view.*
import kotlinx.android.synthetic.main.item_heatmap_yaxis.view.*
import org.rfcx.companion.R

class ArchivedHeatmapAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        const val NORMAL_CELL = 1
        const val Y_AXIS_CELL = 2
    }

    private var data = listOf<HeatmapItem>()

    fun setData(items: List<HeatmapItem>) {
        data = items
        notifyDataSetChanged()
    }

    override fun getItemViewType(position: Int): Int {
        return when (data[position]) {
            is HeatmapItem.YAxis -> Y_AXIS_CELL
            else -> NORMAL_CELL
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            Y_AXIS_CELL -> YAxisHeatmapViewHolder(
                LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_heatmap_yaxis, parent, false)
            )
            else -> NormalHeatmapViewHolder(
                LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_heatmap_normal, parent, false)
            )
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        holder.setIsRecyclable(false)
        when (holder.itemViewType) {
            Y_AXIS_CELL -> (holder as YAxisHeatmapViewHolder).bind(data[position] as HeatmapItem.YAxis)
            else -> (holder as NormalHeatmapViewHolder).bind(data[position] as HeatmapItem.Normal)
        }
    }

    override fun getItemCount(): Int = data.size

    inner class NormalHeatmapViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val valueText = itemView.normalValue
        fun bind(item: HeatmapItem.Normal) {
            val countAsPercent = (item.value.toFloat() / item.maximum.toFloat()) * 100
            when{
                countAsPercent >= 96 -> {
                    itemView.setBackgroundResource(R.color.colorPrimary)
                }
                countAsPercent >= 76 -> {
                    itemView.setBackgroundResource(R.color.yellow)
                }
                countAsPercent >= 51 -> {
                    itemView.setBackgroundResource(R.color.orange)
                }
                countAsPercent > 0 -> {
                    itemView.setBackgroundResource(R.color.red)
                }
                else -> {
                    itemView.setBackgroundResource(R.color.backgroundColor)
                }
            }
            if (item.value != 0) {
                valueText.text = item.value.toString()
            }
        }
    }

    inner class YAxisHeatmapViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val labelText = itemView.yLabelTextView
        fun bind(item: HeatmapItem.YAxis) {
            labelText.text = item.label
        }
    }

}

sealed class HeatmapItem {
    data class Normal(val value: Int, val maximum: Int) : HeatmapItem()
    data class YAxis(val label: String) : HeatmapItem()
}
