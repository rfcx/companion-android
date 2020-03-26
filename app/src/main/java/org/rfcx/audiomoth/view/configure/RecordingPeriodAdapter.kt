package org.rfcx.audiomoth.view.configure

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.recording_period_layout.view.*
import org.rfcx.audiomoth.R

class RecordingPeriodAdapter(private val listener: OnItemClickListener) :
    RecyclerView.Adapter<RecordingPeriodAdapter.RecordingPeriodViewHolder>() {
    var items: ArrayList<String> = arrayListOf()
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecordingPeriodViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val view = inflater.inflate(R.layout.recording_period_layout, parent, false)
        return RecordingPeriodViewHolder(view)
    }

    override fun onBindViewHolder(holder: RecordingPeriodViewHolder, position: Int) {
        val item = items[position]
        holder.bind(item)
        holder.itemView.deleteImageView.setOnClickListener {
            listener.onItemClick(position)
        }
    }

    override fun getItemCount(): Int = items.size

    inner class RecordingPeriodViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val timeTextView = itemView.timeTextView

        fun bind(item: String) {
            timeTextView.text = item
        }
    }
}