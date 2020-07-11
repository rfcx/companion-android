package org.rfcx.audiomoth.view.detail

import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.item_timeline.view.*
import org.rfcx.audiomoth.R


class TimeLineAdapter : RecyclerView.Adapter<TimeLineAdapter.TimeLineViewHolder>() {

    var items: ArrayList<String> = arrayListOf()
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): TimeLineViewHolder {
        val view = View.inflate(parent.context, R.layout.item_timeline, null);
        return TimeLineViewHolder(view, viewType)
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: TimeLineViewHolder, position: Int) {
        holder.bind(items[position])
    }

    class TimeLineViewHolder(itemView: View, viewType: Int) :
        RecyclerView.ViewHolder(itemView) {
        private val customRecording = itemView.text_timeline_title
        private val timeline = itemView.timeline

        fun bind(item: String) {
            customRecording.text = item
        }

        init {
            timeline.initLine(viewType)
        }
    }
}
