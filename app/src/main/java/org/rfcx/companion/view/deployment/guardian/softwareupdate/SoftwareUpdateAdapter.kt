package org.rfcx.companion.view.deployment.guardian.softwareupdate

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.widget.AppCompatImageView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.expandable_child_item.view.*
import kotlinx.android.synthetic.main.expandable_parent_item.view.*
import kotlinx.android.synthetic.main.item_guardian_hotspot.view.*
import org.rfcx.companion.R
import org.rfcx.companion.entity.Software

class SoftwareUpdateAdapter(
    private var childrenClickedListener: ChildrenClickedListener,
    softwares: List<Software>
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private var softwareUpdateStateModelList = mutableListOf<ExpandableSoftwareUpdateModel>()
    private val selectedSoftwares = mutableMapOf<String, String>()

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
                (holder as SoftwareStateChildViewHolder).apkVersion.text = row.softwareChild.version
                holder.apkVersion.apply {
                    if (selectedSoftwares[row.softwareChild.parent] == row.softwareChild.path) {
                        setTextColor(ContextCompat.getColor(this.context, R.color.colorPrimary))
                        setCompoundDrawablesWithIntrinsicBounds(
                            0,
                            0,
                            R.drawable.ic_hotspot_selected,
                            0
                        )

                    } else {
                        setTextColor(ContextCompat.getColor(this.context, R.color.text_secondary))
                        setCompoundDrawablesWithIntrinsicBounds(
                            0,
                            0,
                            0,
                            0
                        )

                    }
                }

                holder.layout.setOnClickListener {
                    if (row.softwareChild.path == selectedSoftwares[row.softwareChild.parent]) {
                        selectedSoftwares[row.softwareChild.parent] = "" // To make it empty
                    } else {
                        selectedSoftwares[row.softwareChild.parent] = row.softwareChild.path
                    }
                    notifyDataSetChanged()
                    childrenClickedListener.onItemClick(selectedSoftwares.filter { it.value != "" })
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
                for (child in row.softwareParent.members) {
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
    fun onItemClick(selectedSoftwares: Map<String, String>)
}
