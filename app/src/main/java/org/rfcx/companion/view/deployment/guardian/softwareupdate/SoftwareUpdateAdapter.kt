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
    var countryStateModelList: MutableList<ExpandableCountryModel>
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    private var isFirstItemExpanded: Boolean = true
    private var actionLock = false
    lateinit var countryName: String
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            ExpandableCountryModel.PARENT -> {
                CountryStateParentViewHolder(
                    LayoutInflater.from(parent.context).inflate(
                        R.layout.expandable_parent_item, parent, false
                    )
                )
            }

            ExpandableCountryModel.CHILD -> {
                CountryStateChildViewHolder(
                    LayoutInflater.from(parent.context).inflate(
                        R.layout.expandable_child_item, parent, false
                    )
                )
            }

            else -> {
                CountryStateParentViewHolder(
                    LayoutInflater.from(parent.context).inflate(
                        R.layout.expandable_parent_item, parent, false
                    )
                )
            }
        }
    }

    override fun getItemCount(): Int = countryStateModelList.size

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val row = countryStateModelList[position]
        when (row.type) {
            ExpandableCountryModel.PARENT -> {
                (holder as CountryStateParentViewHolder).countryName.text = row.countryParent.country
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

            ExpandableCountryModel.CHILD -> {
                (holder as CountryStateChildViewHolder).stateName.text = row.countryChild.name
                holder.capitalImage.text = row.countryChild.capital
            }
        }

    }

    override fun getItemViewType(position: Int): Int = countryStateModelList[position].type

    private fun expandRow(position: Int){
        val row = countryStateModelList[position]
        var nextPosition = position
        when (row.type) {
            ExpandableCountryModel.PARENT -> {
                for(child in row.countryParent.states){
                    countryStateModelList.add(++nextPosition, ExpandableCountryModel(ExpandableCountryModel.CHILD, child))
                }
                notifyDataSetChanged()
            }
            ExpandableCountryModel.CHILD -> {
                notifyDataSetChanged()
            }
        }
    }

    private fun collapseRow(position: Int){
        val row = countryStateModelList[position]
        var nextPosition = position + 1
        when (row.type) {
            ExpandableCountryModel.PARENT -> {
                outerloop@ while (true) {
                    //  println("Next Position during Collapse $nextPosition size is ${shelfModelList.size} and parent is ${shelfModelList[nextPosition].type}")

                    if (nextPosition == countryStateModelList.size || countryStateModelList[nextPosition].type == ExpandableCountryModel.PARENT) {
                        /* println("Inside break $nextPosition and size is ${closedShelfModelList.size}")
                         closedShelfModelList[closedShelfModelList.size-1].isExpanded = false
                         println("Modified closedShelfModelList ${closedShelfModelList.size}")*/
                        break@outerloop
                    }

                    countryStateModelList.removeAt(nextPosition)
                }

                notifyDataSetChanged()
            }
        }
    }

    class CountryStateParentViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        internal var layout = itemView.country_item_parent_container
        internal var countryName: TextView = itemView.country_name
        internal var closeImage = itemView.close_arrow
        internal var upArrowImg = itemView.up_arrow

    }

    class CountryStateChildViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        internal var layout = itemView.country_item_child_container
        internal var stateName: TextView = itemView.state_name
        internal var capitalImage = itemView.capital_name

    }
}

data class StateCapital(
    val countries: List<Country>
) {
    data class Country(
        val country: String, // India
        val states: List<State>
    ) {
        data class State(
            val capital: String, // Hyderabad
            val name: String // Telangana
        )
    }
}


class ExpandableCountryModel {
    companion object {
        const val PARENT = 1
        const val CHILD = 2

    }

    lateinit var countryParent: StateCapital.Country
    var type: Int
    lateinit var countryChild: StateCapital.Country.State
    var isExpanded: Boolean
    private var isCloseShown: Boolean


    constructor(
        type: Int,
        countryParent: StateCapital.Country,
        isExpanded: Boolean = false,
        isCloseShown: Boolean = false
    ) {
        this.type = type
        this.countryParent = countryParent
        this.isExpanded = isExpanded
        this.isCloseShown = isCloseShown

    }


    constructor(
        type: Int,
        countryChild: StateCapital.Country.State,
        isExpanded: Boolean = false,
        isCloseShown: Boolean = false
    ) {
        this.type = type
        this.countryChild = countryChild
        this.isExpanded = isExpanded
        this.isCloseShown = isCloseShown


    }
}

interface CountryClickedListener {
    fun onItemClick(countryName: String, countryChild: StateCapital.Country.State)
}
