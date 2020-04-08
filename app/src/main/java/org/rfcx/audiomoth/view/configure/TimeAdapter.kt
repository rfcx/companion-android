package org.rfcx.audiomoth.view.configure

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.time_item.view.*
import org.rfcx.audiomoth.R

class TimeAdapter(private val listener: OnItemClickListener) :
    RecyclerView.Adapter<TimeAdapter.TimeAdapterViewHolder>() {

    var items: ArrayList<String> = arrayListOf()
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
        val item = items[position]
        holder.bind(item)
        holder.itemView.timeItem.setOnClickListener {
            listener.onTimeItemClick(item)
        }
    }

    override fun getItemCount(): Int = items.size

    inner class TimeAdapterViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val time = itemView.timeItem

        fun bind(item: String) {
            time.text = item
        }
    }
}