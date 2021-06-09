package org.rfcx.companion.view.profile.offlinemap

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.item_location_group.view.*
import org.rfcx.companion.R
import org.rfcx.companion.entity.OfflineMapState
import org.rfcx.companion.entity.Project
import org.rfcx.companion.util.Preferences

class ProjectOfflineMapAdapter(private val projectOfflineMapListener: ProjectOfflineMapListener) :
    RecyclerView.Adapter<ProjectOfflineMapAdapter.ProjectOfflineMapViewHolder>() {
    var items: List<OfflineMapItem> = arrayListOf()
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ProjectOfflineMapAdapter.ProjectOfflineMapViewHolder {
        val view =
            LayoutInflater.from(parent.context).inflate(R.layout.item_location_group, parent, false)
        return ProjectOfflineMapViewHolder(view)
    }

    override fun onBindViewHolder(
        holder: ProjectOfflineMapAdapter.ProjectOfflineMapViewHolder,
        position: Int
    ) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    inner class ProjectOfflineMapViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val locationGroupTextView = itemView.locationGroupTextView
        private val downloadedTextView = itemView.downloadedTextView
        private val downloadButton = itemView.downloadButton

        fun bind(item: OfflineMapItem) {
            downloadButton.visibility =
                if (item.project.maxLatitude != null && item.project.maxLatitude != 0.0 && item.project.offlineMapState != OfflineMapState.DOWNLOADED_STATE.key) View.VISIBLE else View.GONE
            locationGroupTextView.text = item.project.name

            downloadedTextView.visibility = if (item.percentage != null) View.VISIBLE else View.GONE
            downloadedTextView.text = item.percentage.toString()

            downloadButton.setOnClickListener {
                projectOfflineMapListener.onDownloadClicked(item.project)
            }

            val preferences = Preferences.getInstance(itemView.context)
            if (preferences.getString(Preferences.OFFLINE_MAP_NAME) == item.project.name) {
                setViewMapOffline(itemView)
            }

            if (item.project.offlineMapState == OfflineMapState.DOWNLOADED_STATE.key) {
                preferences.clearOfflineMapName()
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

interface ProjectOfflineMapListener {
    fun onDownloadClicked(project: Project)
}

data class OfflineMapItem(val project: Project = Project(), val percentage: Int? = null)
