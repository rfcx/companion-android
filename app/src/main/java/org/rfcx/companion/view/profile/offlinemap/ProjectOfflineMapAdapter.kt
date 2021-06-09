package org.rfcx.companion.view.profile.offlinemap

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.item_location_group.view.*
import org.rfcx.companion.R
import org.rfcx.companion.entity.OfflineMapState
import org.rfcx.companion.entity.Project

class ProjectOfflineMapAdapter(private val projectOfflineMapListener: ProjectOfflineMapListener) :
    RecyclerView.Adapter<ProjectOfflineMapAdapter.ProjectOfflineMapViewHolder>() {
    var items: List<OfflineMapItem> = arrayListOf()
        set(value) {
            field = value
            notifyDataSetChanged()
        }
    var hideDownloadButton = false

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
            locationGroupTextView.text = item.project.name

            downloadButton.setOnClickListener {
                projectOfflineMapListener.onDownloadClicked(item.project)
            }

            setViewMapOffline(itemView, item.project)
            downloadedTextView.text =
                if (item.project.offlineMapState != OfflineMapState.DOWNLOADED_STATE.key) "Downloading..." else "Downloaded"// Todo:: change to show %
            if (hideDownloadButton) {
                downloadButton.visibility = View.GONE
            }
        }
    }

    private fun setViewMapOffline(itemView: View, project: Project) {
        val offlineMapProgress = itemView.offlineMapProgress
        val downloadedTextView = itemView.downloadedTextView
        val downloadButton = itemView.downloadButton
        when (project.offlineMapState) {
            OfflineMapState.DOWNLOAD_STATE.key -> {
                offlineMapProgress.visibility = View.GONE
                downloadedTextView.visibility = View.GONE
                downloadButton.visibility =
                    if (project.maxLatitude != null && project.maxLatitude != 0.0 && project.offlineMapState != OfflineMapState.DOWNLOADED_STATE.key) View.VISIBLE else View.GONE
            }
            OfflineMapState.DOWNLOADING_STATE.key -> {
                offlineMapProgress.visibility = View.VISIBLE
                downloadedTextView.visibility = View.VISIBLE
                downloadButton.visibility = View.GONE
            }
            OfflineMapState.DOWNLOADED_STATE.key -> {
                offlineMapProgress.visibility = View.GONE
                downloadedTextView.visibility = View.VISIBLE
                downloadButton.visibility = View.GONE
            }
        }
    }
}

interface ProjectOfflineMapListener {
    fun onDownloadClicked(project: Project)
}

data class OfflineMapItem(val project: Project = Project(), val percentage: Int? = null)
