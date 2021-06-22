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

class ProjectOfflineMapAdapter(var items: List<Project>, private val projectOfflineMapListener: ProjectOfflineMapListener) :
    RecyclerView.Adapter<ProjectOfflineMapAdapter.ProjectOfflineMapViewHolder>() {

    companion object {
        const val PROGRESS = "progress"
    }

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
        val project = items[position]
        with(holder.itemView) {
            locationGroupTextView.text = project.name

            downloadButton.setOnClickListener {
                projectOfflineMapListener.onDownloadClicked(project)
            }

            setViewMapOffline(this, project)
            downloadedTextView.isVisible = project.offlineMapState == OfflineMapState.DOWNLOADED_STATE.key
            downloadedTextView.text = "Downloaded"  // TODO change to delete button
            if (hideDownloadButton) {
                downloadButton.visibility = View.GONE
            }
        }
    }

    override fun onBindViewHolder(holder: ProjectOfflineMapAdapter.ProjectOfflineMapViewHolder, position: Int, payloads: MutableList<Any>) {
        super.onBindViewHolder(holder, position, payloads)
        if (payloads.firstOrNull() != null) {
            with(holder.itemView) {
                (payloads.first() as Bundle).getInt(PROGRESS).also {
                    downloadedTextView.isVisible = it < 99
                    downloadedTextView.text = "$it %"
                }
            }
        }
    }

    fun setDownloading(project: Project) {
        notifyItemChanged(items.indexOf(project))
    }

    fun setProgress(project: Project, progress: Int) {
        notifyItemChanged(items.indexOf(project), Bundle().apply { putInt(PROGRESS, progress) })
    }

    override fun getItemCount(): Int = items.size

    inner class ProjectOfflineMapViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)

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
    fun onDeleteClicked(project: Project)
}
