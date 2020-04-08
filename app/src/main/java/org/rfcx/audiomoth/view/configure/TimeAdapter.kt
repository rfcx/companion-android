package org.rfcx.audiomoth.view.configure

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.time_item.view.*
import org.rfcx.audiomoth.R
import org.rfcx.audiomoth.util.getIntColor

class TimeAdapter(private val listener: OnItemClickListener, private val context: Context?) :
    RecyclerView.Adapter<TimeAdapter.TimeAdapterViewHolder>() {

    var items: MutableMap<String, Boolean> = mutableMapOf()
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TimeAdapterViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val view = inflater.inflate(R.layout.time_item, parent, false)
        return TimeAdapterViewHolder(view)
    }

    override fun onBindViewHolder(holder: TimeAdapterViewHolder, position: Int) {
        val timeList = arrayListOf<String>()
        val timeStatus = arrayListOf<Boolean>()
        items.forEach {
            timeList.add(it.key)
            timeStatus.add(it.value)
        }
        val item = timeList[position]
        val status = timeStatus[position]
        holder.bind(item, status)
        holder.itemView.timeItem.setOnClickListener {
            listener.onTimeItemClick(item)
        }
    }

    override fun getItemCount(): Int = items.size

    inner class TimeAdapterViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val time = itemView.timeItem

        fun bind(item: String, status: Boolean) {
            time.text = item

            if (context != null) {
                if (status) {
                    time.setBackgroundColor(context.getIntColor(R.color.colorPrimary))
                } else {
                    time.setBackgroundColor(context.getIntColor(R.color.transparent))
                }
            }
        }
    }
}