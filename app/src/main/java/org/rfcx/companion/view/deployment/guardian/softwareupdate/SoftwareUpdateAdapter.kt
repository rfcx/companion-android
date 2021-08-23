package org.rfcx.companion.view.deployment.guardian.softwareupdate

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.expandable_child_item.view.*
import kotlinx.android.synthetic.main.expandable_parent_item.view.*
import org.rfcx.companion.R

class SoftwareUpdateAdapter(
    var countryClickedListener: CountryClickedListener,
    var softwareUpdateStateModelList: MutableList<ExpandableSoftwareUpdateModel>
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
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
                (holder as SoftwareStateParentViewHolder).appName.text = row.softwareParent.appName
                holder.itemView.setOnClickListener {
                    if (row.isExpanded) {
                        row.isExpanded = false
                        collapseRow(position)
                        holder.itemView.close_arrow.rotation = 0F

                    } else {
                        row.isExpanded = true
                        expandRow(position)
                        holder.itemView.close_arrow.rotation = 180F
                    }
                }
            }

            ExpandableSoftwareUpdateModel.CHILD -> {
                (holder as SoftwareStateChildViewHolder).apkVersion.text = row.softwareChild.name

                holder.layout.setOnClickListener {
                    countryClickedListener.onItemClick(row.softwareChild.name)
                }
            }
        }

    }

    override fun getItemViewType(position: Int): Int = softwareUpdateStateModelList[position].type

    private fun expandRow(position: Int) {
        val row = softwareUpdateStateModelList[position]
        var nextPosition = position
        when (row.type) {
            ExpandableSoftwareUpdateModel.PARENT -> {
                for (child in row.softwareParent.apkVersions) {
                    softwareUpdateStateModelList.add(
                        ++nextPosition,
                        ExpandableSoftwareUpdateModel(ExpandableSoftwareUpdateModel.CHILD, child)
                    )
                }
                notifyDataSetChanged()
            }
            ExpandableSoftwareUpdateModel.CHILD -> {
                notifyDataSetChanged()
            }
        }
    }

    private fun collapseRow(position: Int) {
        val row = softwareUpdateStateModelList[position]
        val nextPosition = position + 1
        when (row.type) {
            ExpandableSoftwareUpdateModel.PARENT -> {
                outerloop@ while (true) {
                    if (nextPosition == softwareUpdateStateModelList.size || softwareUpdateStateModelList[nextPosition].type == ExpandableSoftwareUpdateModel.PARENT) {
                        break@outerloop
                    }
                    softwareUpdateStateModelList.removeAt(nextPosition)
                }
                notifyDataSetChanged()
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
    }
}

data class StateSoftwareUpdate(
    val softwares: List<Software>
) {
    data class Software(
        val appName: String,
        val apkVersions: List<ApkVersion>
    ) {
        data class ApkVersion(
            val name: String
        )
    }
}

class ExpandableSoftwareUpdateModel {
    companion object {
        const val PARENT = 1
        const val CHILD = 2
    }

    lateinit var softwareParent: StateSoftwareUpdate.Software
    var type: Int
    lateinit var softwareChild: StateSoftwareUpdate.Software.ApkVersion
    var isExpanded: Boolean
    private var isCloseShown: Boolean

    constructor(
        type: Int,
        softwareParent: StateSoftwareUpdate.Software,
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
        softwareChild: StateSoftwareUpdate.Software.ApkVersion,
        isExpanded: Boolean = false,
        isCloseShown: Boolean = false
    ) {
        this.type = type
        this.softwareChild = softwareChild
        this.isExpanded = isExpanded
        this.isCloseShown = isCloseShown
    }
}

interface CountryClickedListener {
    fun onItemClick(apkVersion: String)
}
