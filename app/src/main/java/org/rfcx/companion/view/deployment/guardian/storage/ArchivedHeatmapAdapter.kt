package org.rfcx.companion.view.deployment.guardian.storage

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import org.rfcx.companion.R

class ArchivedHeatmapAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        const val NORMAL_CELL = 1
        const val X_AXIS_CELL = 2
        const val Y_AXIS_CELL = 3
    }

    private var data = listOf<HeatmapItem>()

    override fun getItemViewType(position: Int): Int {
        return when (data[position]) {
            is HeatmapItem.XAxis -> X_AXIS_CELL
            is HeatmapItem.YAxis -> Y_AXIS_CELL
            else -> NORMAL_CELL
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            X_AXIS_CELL -> XAxisHeatmapViewHolder(
                LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_checklist_step, parent, false)
            )
            Y_AXIS_CELL -> YAxisHeatmapViewHolder(
                LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_checklist_step, parent, false)
            )
            else -> NormalHeatmapViewHolder(
                LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_checklist_header, parent, false)
            )
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder.itemViewType) {
            X_AXIS_CELL -> (holder as XAxisHeatmapViewHolder).bind(data[position] as HeatmapItem.XAxis)
            Y_AXIS_CELL -> (holder as YAxisHeatmapViewHolder).bind(data[position] as HeatmapItem.YAxis)
            else -> (holder as NormalHeatmapViewHolder).bind(data[position] as HeatmapItem.Normal)
        }
    }

    override fun getItemCount(): Int = data.size

    inner class NormalHeatmapViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        fun bind(item: HeatmapItem.Normal) {

        }
    }

    inner class XAxisHeatmapViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        fun bind(item: HeatmapItem.XAxis) {

        }
    }

    inner class YAxisHeatmapViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        fun bind(item: HeatmapItem.YAxis) {

        }
    }

}

sealed class HeatmapItem {
    data class Normal(val value: Int) : HeatmapItem()
    data class XAxis(val label: String, val value: Int) : HeatmapItem()
    data class YAxis(val label: String, val value: Int) : HeatmapItem()
}
