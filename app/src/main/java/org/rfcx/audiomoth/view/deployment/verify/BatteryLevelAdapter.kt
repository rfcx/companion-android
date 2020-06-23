package org.rfcx.audiomoth.view.deployment.verify

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.item_battery_level.view.*
import kotlinx.android.synthetic.main.time_item.view.*
import org.rfcx.audiomoth.R

class BatteryLevelAdapter(
    private val listener: OnItemClickListener,
    private val context: Context?
) :
    RecyclerView.Adapter<BatteryLevelAdapter.BatteryLevelAdapterViewHolder>() {

    var items: ArrayList<String> = arrayListOf()
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): BatteryLevelAdapterViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val view = inflater.inflate(R.layout.item_battery_level, parent, false)
        return BatteryLevelAdapterViewHolder(view)
    }

    override fun onBindViewHolder(holder: BatteryLevelAdapterViewHolder, position: Int) {
        holder.bind(items[position])

        holder.itemView.setOnClickListener {
            listener.onLevelItemClick(items[position])
        }
    }

    override fun getItemCount(): Int = items.size

    inner class BatteryLevelAdapterViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val levelTextView = itemView.levelTextView

        fun bind(level: String) {
            levelTextView.text = level
        }
    }
}