package org.rfcx.audiomoth.view.dashboard

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.dashboard_stream_layout.view.*
import org.rfcx.audiomoth.R

class DashboardStreamAdapter :
    RecyclerView.Adapter<DashboardStreamAdapter.DashboardStreamViewHolder>() {
    var items: ArrayList<String> = arrayListOf()
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    var onDashboardClick: OnDashboardClickListener? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DashboardStreamViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val view = inflater.inflate(R.layout.dashboard_stream_layout, parent, false)
        return DashboardStreamViewHolder(view)
    }

    override fun onBindViewHolder(holder: DashboardStreamViewHolder, position: Int) {
        val item = items[position]
        holder.bind(item)
        holder.itemView.setOnClickListener {
            onDashboardClick?.onDashboardClick(item)
        }
    }

    override fun getItemCount(): Int = items.size

    inner class DashboardStreamViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val streamTextView = itemView.streamTextView

        fun bind(item: String) {
            streamTextView.text = item
        }
    }
}