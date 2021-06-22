package org.rfcx.companion.view.profile.offlinemap

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.item_location_group.view.*
import org.rfcx.companion.R
import org.rfcx.companion.entity.OfflineMapState
import org.rfcx.companion.entity.Project

class ProjectOfflineMapAdapter(var items: List<OfflineMapItem>, private val projectOfflineMapListener: ProjectOfflineMapListener) :
    RecyclerView.Adapter<ProjectOfflineMapAdapter.ProjectOfflineMapViewHolder>() {

    var hideDownloadButton: Boolean = false
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

    override fun onBindViewHolder(holder: ProjectOfflineMapAdapter.ProjectOfflineMapViewHolder, position: Int) {
        val dummy = items[position]
        with(holder.itemView) {
            locationGroupTextView.text = dummy.project.name

            downloadButton.setOnClickListener {
                projectOfflineMapListener.onDownloadClicked(dummy.project)
            }

            setViewMapOffline(this, dummy.project)
            downloadedTextView.isVisible = dummy.project.offlineMapState == OfflineMapState.DOWNLOADED_STATE.key
            downloadedTextView.text = "Downloaded"
            if (hideDownloadButton) {
                downloadButton.visibility = View.GONE
            }
        }
    }

    override fun onBindViewHolder(holder: ProjectOfflineMapAdapter.ProjectOfflineMapViewHolder, position: Int, payloads: MutableList<Any>) {
        super.onBindViewHolder(holder, position, payloads)
        if (payloads.firstOrNull() != null) {
            with(holder.itemView) {
                (payloads.first() as Bundle).getInt("progress").also {
                    downloadedTextView.isVisible = it < 99
                    downloadedTextView.text = "$it %"
                }
            }
        }
    }

    fun setDownloading(dummy: OfflineMapItem) {
        notifyItemChanged(items.indexOf(dummy))
    }

    fun setProgress(dummy: OfflineMapItem, progress: Int) {
        getDummy(dummy)?.percentage = progress
        notifyItemChanged(items.indexOf(dummy), Bundle().apply { putInt("progress", progress) })
    }

    private fun getDummy(dummy: OfflineMapItem) = items.find { dummy.project.id == it.project.id }

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

data class OfflineMapItem(val project: Project = Project(), var percentage: Int? = null)
