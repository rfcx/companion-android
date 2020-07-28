package org.rfcx.audiomoth.view.detail

import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.item_timeline.view.*
import org.rfcx.audiomoth.R
import org.rfcx.audiomoth.util.getIntColor

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
        val view = View.inflate(parent.context, R.layout.item_timeline, null)
        return TimeLineViewHolder(view, viewType)
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: TimeLineViewHolder, position: Int) {
        holder.bind(items[position], items)
    }

    class TimeLineViewHolder(itemView: View, viewType: Int) :
        RecyclerView.ViewHolder(itemView) {
        private val customRecording = itemView.timelineTextView
        private val timeline = itemView.timeline
        private val viewTypeViewHolder = viewType
        private val context = itemView.context

        fun bind(item: String, items: ArrayList<String>) {
            customRecording.text = item
            timeline.initLine(viewTypeViewHolder)

            if (items[0] == item) {
                timeline.setStartLineColor(context.getIntColor(R.color.white), viewTypeViewHolder)
            } else {
                timeline.setStartLineColor(
                    context.getIntColor(R.color.colorPrimary),
                    viewTypeViewHolder
                )
            }

            if (items.last() == item) {
                timeline.setEndLineColor(context.getIntColor(R.color.white), viewTypeViewHolder)
            } else {
                timeline.setEndLineColor(
                    context.getIntColor(R.color.colorPrimary),
                    viewTypeViewHolder
                )
            }
        }
    }
}
