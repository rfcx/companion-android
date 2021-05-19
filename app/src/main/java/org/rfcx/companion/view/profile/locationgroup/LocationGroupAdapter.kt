package org.rfcx.companion.view.profile.locationgroup

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.item_location_group.view.*
import org.rfcx.companion.R
import org.rfcx.companion.entity.Project
import org.rfcx.companion.entity.Screen

class LocationGroupAdapter(private val locationGroupListener: LocationGroupListener) :
    RecyclerView.Adapter<LocationGroupAdapter.LocationGroupAdapterViewHolder>() {
    var selectedGroup: String = ""
    var screen: String = ""
    var items: List<Project> = arrayListOf()
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    fun removeGroup(id: Int) {
        items = items.filter { it.id != id }
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
            locationGroupListener.onClicked(items[position])
        }
        if (items[position].id != -1) {
            holder.itemView.setOnLongClickListener {
                locationGroupListener.onLongClicked(items[position])
                true
            }
        }
    }

    inner class LocationGroupAdapterViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val locationGroupTextView = itemView.locationGroupTextView
        private val checkImageView = itemView.checkImageView
        private val downloadButton = itemView.downloadButton

        fun bind(project: Project) {
            if (screen != Screen.PROFILE.id) {
                checkImageView.visibility =
                    if (project.name == selectedGroup) View.VISIBLE else View.GONE
            }
            downloadButton.visibility =
                if (screen == Screen.OFFLINE_MAP.id) View.VISIBLE else View.GONE
            locationGroupTextView.text = project.name
        }
    }
}

interface LocationGroupListener {
    fun onClicked(group: Project)
    fun onLongClicked(group: Project)
}
