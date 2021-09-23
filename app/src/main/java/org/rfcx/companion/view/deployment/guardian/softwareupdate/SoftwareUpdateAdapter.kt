package org.rfcx.companion.view.deployment.guardian.softwareupdate

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.expandable_child_item.view.*
import kotlinx.android.synthetic.main.expandable_parent_item.view.*
import kotlinx.android.synthetic.main.item_guardian_hotspot.view.*
import org.rfcx.companion.R
import org.rfcx.companion.entity.Software
import org.rfcx.companion.util.file.APKUtils.calculateVersionValue

class SoftwareUpdateAdapter(
    private var childrenClickedListener: ChildrenClickedListener,
    softwares: List<Software>
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private var softwareUpdateStateModelList = mutableListOf<ExpandableSoftwareUpdateModel>()

    var guardianSoftwareVersion = mapOf<String, String>()

    private var needLoading = false

    init {
        val softwaresGrouped = softwares.sortedBy { it.name.value }.groupBy { it.name }
        softwaresGrouped.forEach { software ->
            softwareUpdateStateModelList.add(
                ExpandableSoftwareUpdateModel(
                    1,
                    StateSoftwareUpdate(
                        software.key.value,
                        software.value.map {
                            StateSoftwareUpdate.SoftwareChildren(
                                software.key.value,
                                it.version,
                                it.path
                            )
                        })
                )
            )
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            ExpandableSoftwareUpdateModel.PARENT -> {
                SoftwareStateParentViewHolder(
                    LayoutInflater.from(parent.context).inflate(
                        R.layout.expandable_parent_item, parent, false
                    )
                )
            }

            ExpandableSoftwareUpdateModel.CHILD -> {
                SoftwareStateChildViewHolder(
                    LayoutInflater.from(parent.context).inflate(
                        R.layout.expandable_child_item, parent, false
                    )
                )
            }

            else -> {
                SoftwareStateParentViewHolder(
                    LayoutInflater.from(parent.context).inflate(
                        R.layout.expandable_parent_item, parent, false
                    )
                )
            }
        }
    }

    override fun getItemCount(): Int = softwareUpdateStateModelList.size

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val row = softwareUpdateStateModelList[position]

        when (row.type) {
            ExpandableSoftwareUpdateModel.PARENT -> {
                (holder as SoftwareStateParentViewHolder).appName.text = row.softwareParent.name
                expandRow(position)
            }

            ExpandableSoftwareUpdateModel.CHILD -> {
                (holder as SoftwareStateChildViewHolder).apkVersion.text = row.softwareChild.version
                holder.apkVersion.apply {
                    val installedVersion = guardianSoftwareVersion[row.softwareChild.parent]
                    if (!needLoading) {
                        if (installedVersion != null && calculateVersionValue(installedVersion) >= calculateVersionValue(row.softwareChild.version)) {
                            holder.apkSendButton.visibility = View.GONE
                            holder.apkUpToDateText.visibility = View.VISIBLE
                        } else {
                            holder.apkSendButton.visibility = View.VISIBLE
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
                    childrenClickedListener.onItemClick(row.softwareChild)
                }
            }
        }

    }

    fun showLoading() {
        needLoading = true
    }

    fun hideLoading() {
        needLoading = false
    }

    fun getLoading(): Boolean = needLoading

    override fun getItemViewType(position: Int): Int = softwareUpdateStateModelList[position].type

    private fun expandRow(position: Int) {
        val row = softwareUpdateStateModelList[position]
        var nextPosition = position
        when (row.type) {
            ExpandableSoftwareUpdateModel.PARENT -> {
                for (child in row.softwareParent.members) {
                    softwareUpdateStateModelList.add(
                        ++nextPosition,
                        ExpandableSoftwareUpdateModel(ExpandableSoftwareUpdateModel.CHILD, child)
                    )
                }
            }
        }
    }

    class SoftwareStateParentViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        internal var layout = itemView.country_item_parent_container
        internal var appName: TextView = itemView.appNameTextView
    }

    class SoftwareStateChildViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        internal var layout = itemView.country_item_child_container
        internal var apkVersion: TextView = itemView.apkVersionTextView
        internal var apkSendButton: Button = itemView.apkSendButton
        internal var apkUpToDateText: TextView = itemView.apkUpToDateTextView
        internal var apkLoading: ProgressBar = itemView.apkLoading
    }
}

data class StateSoftwareUpdate(
    val name: String,
    val members: List<SoftwareChildren>
) {
    data class SoftwareChildren(
        val parent: String,
        val version: String,
        val path: String
    )
}

class ExpandableSoftwareUpdateModel {
    companion object {
        const val PARENT = 1
        const val CHILD = 2
    }

    lateinit var softwareParent: StateSoftwareUpdate
    var type: Int
    lateinit var softwareChild: StateSoftwareUpdate.SoftwareChildren
    var isExpanded: Boolean
    private var isCloseShown: Boolean

    constructor(
        type: Int,
        softwareParent: StateSoftwareUpdate,
        isExpanded: Boolean = false,
        isCloseShown: Boolean = false
    ) {
        this.type = type
        this.softwareParent = softwareParent
        this.isExpanded = isExpanded
        this.isCloseShown = isCloseShown
    }

    constructor(
        type: Int,
        softwareChild: StateSoftwareUpdate.SoftwareChildren,
        isExpanded: Boolean = false,
        isCloseShown: Boolean = false
    ) {
        this.type = type
        this.softwareChild = softwareChild
        this.isExpanded = isExpanded
        this.isCloseShown = isCloseShown
    }
}

interface ChildrenClickedListener {
    fun onItemClick(selectedSoftware: StateSoftwareUpdate.SoftwareChildren)
}
