package org.rfcx.companion.view.deployment.guardian.softwareupdate

import android.graphics.Color
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
    private var isFirstItemExpanded: Boolean = true
    private var actionLock = false
    lateinit var countryName: String
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
                (holder as SoftwareStateParentViewHolder).countryName.text = row.softwareParent.appName
                holder.closeImage.setOnClickListener {
                    if (row.isExpanded) {
                        row.isExpanded = false
                        collapseRow(position)
                        holder.layout.setBackgroundColor(Color.WHITE)
                    }else{
                        holder.layout.setBackgroundColor(Color.GRAY)
                        row.isExpanded = true
                        holder.upArrowImg.visibility = View.VISIBLE
                        holder.closeImage.visibility = View.GONE
                        expandRow(position)
                    }
                }
                holder.upArrowImg.setOnClickListener{
                    if(row.isExpanded){
                        row.isExpanded = false
                        collapseRow(position)
                        holder.layout.setBackgroundColor(Color.WHITE)
                        holder.upArrowImg.visibility = View.GONE
                        holder.closeImage.visibility = View.VISIBLE

                    }
                }
            }

            ExpandableSoftwareUpdateModel.CHILD -> {
                (holder as SoftwareStateChildViewHolder).stateName.text = row.softwareChild.name
                holder.capitalImage.text = row.softwareChild.capital

                holder.layout.setOnClickListener {
                    val softwareInfo =   holder.stateName.tag
                    countryClickedListener.onItemClick(holder.layout.tag.toString(),
                        softwareInfo as StateSoftwareUpdate.Software.ApkVersion
                    )
                }
            }
        }

    }

    override fun getItemViewType(position: Int): Int = softwareUpdateStateModelList[position].type

    private fun expandRow(position: Int){
        val row = softwareUpdateStateModelList[position]
        var nextPosition = position
        when (row.type) {
            ExpandableSoftwareUpdateModel.PARENT -> {
                for(child in row.softwareParent.apkVersions){
                    softwareUpdateStateModelList.add(++nextPosition, ExpandableSoftwareUpdateModel(ExpandableSoftwareUpdateModel.CHILD, child))
                }
                notifyDataSetChanged()
            }
            ExpandableSoftwareUpdateModel.CHILD -> {
                notifyDataSetChanged()
            }
        }
    }

    private fun collapseRow(position: Int){
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
        internal var countryName: TextView = itemView.country_name
        internal var closeImage = itemView.close_arrow
        internal var upArrowImg = itemView.up_arrow
    }

    class SoftwareStateChildViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        internal var layout = itemView.country_item_child_container
        internal var stateName: TextView = itemView.state_name
        internal var capitalImage = itemView.capital_name
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
            val capital: String,
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
    fun onItemClick(appName: String, softwareChild: StateSoftwareUpdate.Software.ApkVersion)
}
