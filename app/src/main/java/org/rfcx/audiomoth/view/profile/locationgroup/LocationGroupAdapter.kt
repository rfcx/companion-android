package org.rfcx.audiomoth.view.profile.locationgroup

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.item_location_group.view.*
import org.rfcx.audiomoth.R

class LocationGroupAdapter :
    RecyclerView.Adapter<LocationGroupAdapter.LocationGroupAdapterViewHolder>() {

    var items: List<String> = arrayListOf()
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): LocationGroupAdapterViewHolder {
        val view =
            LayoutInflater.from(parent.context).inflate(R.layout.item_location_group, parent, false)
        return LocationGroupAdapterViewHolder(view)
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: LocationGroupAdapterViewHolder, position: Int) {
        holder.bind(items[position])
        holder.itemView.setOnClickListener {
            // TODO: setOnClick
        }
    }

    inner class LocationGroupAdapterViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val locationGroupTextView = itemView.locationGroupTextView

        fun bind(locationGroup: String) {
            locationGroupTextView.text = locationGroup
        }
    }
}