package org.rfcx.companion.view.deployment.guardian.softwareupdate

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.progressindicator.LinearProgressIndicator
import kotlinx.android.synthetic.main.expandable_child_item.view.*
import kotlinx.android.synthetic.main.expandable_parent_item.view.*
import org.rfcx.companion.R
import org.rfcx.companion.entity.Software
import org.rfcx.companion.util.file.APKUtils.calculateVersionValue

class SoftwareUpdateAdapter(
    private var childrenClickedListener: ChildrenClickedListener,
    softwares: List<Software>
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        const val VERSION_ITEM = 1
        const val HEADER_ITEM = 2
    }

    private var softwareUpdateStateModelList = mutableListOf<SoftwareItem>()

    var guardianSoftwareVersion = mapOf<String, String>()

    private var needLoading = false

    var progress = 0
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    init {
        val softwaresGrouped = softwares.sortedBy { it.name.value }.groupBy { it.name }
        softwaresGrouped.forEach { software ->
            softwareUpdateStateModelList.add(SoftwareItem.SoftwareHeader(software.key.value))
            software.value.forEach {
                softwareUpdateStateModelList.add(SoftwareItem.SoftwareVersion(software.key.value, it.version, it.path))
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            VERSION_ITEM -> {
                SoftwareVersionViewHolder(
                    LayoutInflater.from(parent.context).inflate(
                        R.layout.expandable_child_item, parent, false
                    )
                )
            }

            else -> {
                SoftwareHeaderViewHolder(
                    LayoutInflater.from(parent.context).inflate(
                        R.layout.expandable_parent_item, parent, false
                    )
                )
            }
        }
    }

    override fun getItemCount(): Int = softwareUpdateStateModelList.size

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder.itemViewType) {
            VERSION_ITEM -> {
                val versionItem = (softwareUpdateStateModelList[position] as SoftwareItem.SoftwareVersion)
                (holder as SoftwareVersionViewHolder).apkVersion.text = versionItem.version
                holder.apkVersion.apply {
                    val installedVersion = guardianSoftwareVersion[versionItem.parent]
                    holder.apkInstalled.text = context.getString(R.string.installed_software, installedVersion)
                    if (!needLoading) {
                        if (installedVersion != null && calculateVersionValue(installedVersion) >= calculateVersionValue(versionItem.version)) {
                            holder.apkSendButton.visibility = View.GONE
                            holder.apkUpToDateText.visibility = View.VISIBLE
                        } else {
                            holder.apkSendButton.visibility = View.VISIBLE
                            holder.apkSendButton.text = "update to ${versionItem.version}"
                            holder.apkUpToDateText.visibility = View.GONE
                        }
                    }
                }

                holder.apkSendButton.isEnabled = !needLoading
                if (!needLoading && holder.apkLoading.visibility == View.VISIBLE) {
                    holder.apkLoading.visibility = View.GONE
                }
                holder.apkSendButton.setOnClickListener {
                    showLoading()
                    it.visibility = View.GONE
                    holder.apkLoading.visibility = View.VISIBLE
                    childrenClickedListener.onItemClick(versionItem)
                }

                if (progress != 100) {
                    holder.setProgress(progress)
                } else {
                    holder.apkLoading.isIndeterminate = true
                }
            }
            else -> {
                val headerItem = (softwareUpdateStateModelList[position] as SoftwareItem.SoftwareHeader)
                (holder as SoftwareHeaderViewHolder).appName.text = headerItem.name
            }
        }
    }

    fun showLoading() {
        needLoading = true
        notifyDataSetChanged()
    }

    fun hideLoading() {
        needLoading = false
        notifyDataSetChanged()
    }

    fun getLoading(): Boolean = needLoading

    override fun getItemViewType(position: Int): Int {
        return when (softwareUpdateStateModelList[position]) {
            is SoftwareItem.SoftwareVersion -> VERSION_ITEM
            else -> HEADER_ITEM
        }
    }

    class SoftwareHeaderViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        internal var layout = itemView.country_item_parent_container
        internal var appName: TextView = itemView.fileNameTextView
    }

    class SoftwareVersionViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        internal var layout = itemView.country_item_child_container
        internal var apkVersion: TextView = itemView.fileVersionTextView
        internal var apkInstalled: TextView = itemView.fileInstalledVersionTextView
        internal var apkSendButton: Button = itemView.fileSendButton
        internal var apkUpToDateText: TextView = itemView.fileUpToDateTextView
        internal var apkLoading: LinearProgressIndicator = itemView.fileLoading

        fun setProgress(value: Int) {
            apkLoading.setProgressCompat(value, true)
        }
    }
}

sealed class SoftwareItem {
    data class SoftwareHeader(val name: String) : SoftwareItem()
    data class SoftwareVersion(val parent: String, val version: String, val path: String) : SoftwareItem()
}

interface ChildrenClickedListener {
    fun onItemClick(selectedSoftware: SoftwareItem.SoftwareVersion)
}
