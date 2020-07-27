package org.rfcx.audiomoth.view.deployment.configure

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.item_time.view.*
import org.rfcx.audiomoth.R
import org.rfcx.audiomoth.util.getIntColor

class TimeAdapter(private val listener: OnItemClickListener, private val context: Context?) :
    RecyclerView.Adapter<TimeAdapter.TimeAdapterViewHolder>() {

    var items: ArrayList<TimeItem> = arrayListOf()
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TimeAdapterViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val view = inflater.inflate(R.layout.item_time, parent, false)
        return TimeAdapterViewHolder(view)
    }

    override fun onBindViewHolder(holder: TimeAdapterViewHolder, position: Int) {
        holder.bind(items[position])

        holder.itemView.timeItemTextView.setOnClickListener {
            listener.onTimeItemClick(items[position], position)
        }
    }

    override fun getItemCount(): Int = items.size

    inner class TimeAdapterViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val timeTextView = itemView.timeItemTextView

        fun bind(item: TimeItem) {
            timeTextView.text = item.time

            if (context != null) {
                if (item.state) {
                    timeTextView.setBackgroundColor(context.getIntColor(R.color.colorPrimary))
                } else {
                    timeTextView.setBackgroundColor(context.getIntColor(R.color.transparent))
                }
            }
        }
    }
}