package org.rfcx.companion.view.profile.locationgroup

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.item_location_group.view.*
import org.rfcx.companion.R
import org.rfcx.companion.entity.OfflineMapState
import org.rfcx.companion.entity.Project
import org.rfcx.companion.entity.Screen
import org.rfcx.companion.util.Preferences

class LocationGroupAdapter(private val locationGroupListener: LocationGroupListener) :
    RecyclerView.Adapter<LocationGroupAdapter.LocationGroupAdapterViewHolder>() {
    var selectedGroup: String = ""
    var screen: String = ""
    var items: List<Project> = arrayListOf()
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
            locationGroupListener.onClicked(items[position])
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
                if (project.maxLatitude != null && project.maxLatitude != 0.0) View.VISIBLE else View.GONE
            locationGroupTextView.text = project.name

            downloadButton.setOnClickListener {
                locationGroupListener.onDownloadClicked(project)
            }

            val preferences = Preferences.getInstance(itemView.context)
            if (preferences.getString(Preferences.OFFLINE_MAP_NAME) == project.name) {
                setViewMapOffline(itemView)
            }
        }
    }

    private fun setViewMapOffline(itemView: View) {
        val offlineMapProgress = itemView.offlineMapProgress
        val downloadedTextView = itemView.downloadedTextView
        val downloadButton = itemView.downloadButton
        val preferences = Preferences.getInstance(itemView.context)
        when (preferences.getString(Preferences.OFFLINE_MAP_STATE)) {
            OfflineMapState.DOWNLOAD_STATE.key -> {
                offlineMapProgress.visibility = View.GONE
                downloadedTextView.visibility = View.GONE
                downloadButton.visibility = View.VISIBLE
            }
            OfflineMapState.DOWNLOADING_STATE.key -> {
                offlineMapProgress.visibility = View.VISIBLE
                downloadedTextView.visibility = View.VISIBLE
                downloadButton.visibility = View.GONE
            }
            OfflineMapState.DOWNLOADED_STATE.key -> {
                offlineMapProgress.visibility = View.GONE
                downloadedTextView.visibility = View.GONE
                downloadButton.visibility = View.GONE
            }
        }
    }
}

interface LocationGroupListener {
    fun onClicked(group: Project)
    fun onDownloadClicked(project: Project)
}
