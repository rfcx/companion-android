package org.rfcx.audiomoth.view.deployment

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import org.rfcx.audiomoth.R
import org.rfcx.audiomoth.adapter.CheckListItem

class CheckListAdapter(private val onCheckClickListener: (Int) -> Unit) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private var listOfChecks = listOf<CheckListItem>()

    companion object {
        const val CHECK_ITEM = 1
        const val HEADER_ITEM = 2
    }

    fun setCheckList(checks: List<CheckListItem>) {
        listOfChecks = checks
    }

    fun setCheckPassed(number: Int) {
        listOfChecks.filterIsInstance<CheckListItem.CheckItem>()
            .find { it.number == number }?.isPassed = true
        notifyDataSetChanged()
    }

    fun isEveryCheckListPassed(): Boolean {
        return listOfChecks.filterIsInstance<CheckListItem.CheckItem>().filter { it.isRequired }.all { it.isPassed }
    }

    override fun getItemViewType(position: Int): Int {
        return when (listOfChecks[position]) {
            is CheckListItem.CheckItem -> CHECK_ITEM
            else -> HEADER_ITEM
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            CHECK_ITEM -> CheckItemViewHolder(
                LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_checklist_step, parent, false)
            )
            else -> HeaderItemViewHolder(
                LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_checklist_header, parent, false)
            )
        }
    }

    override fun getItemCount(): Int = listOfChecks.size

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder.itemViewType) {
            CHECK_ITEM -> (holder as CheckItemViewHolder).bind(listOfChecks[position] as CheckListItem.CheckItem)
            else -> (holder as HeaderItemViewHolder).bind(listOfChecks[position] as CheckListItem.Header)
        }
    }

    inner class CheckItemViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val checkName = itemView.findViewById<TextView>(R.id.checkName)
        fun bind(check: CheckListItem.CheckItem) {
            checkName.text = check.name

            checkName.setOnClickListener {
                onCheckClickListener(check.number)
            }

            if (check.isPassed) {
                checkName.setTextColor(
                    ContextCompat.getColor(
                        itemView.context,
                        R.color.colorPrimary
                    )
                )
                checkName.setCompoundDrawablesWithIntrinsicBounds(
                    R.drawable.ic_checklist_passed,
                    0,
                    0,
                    0
                )
            } else {
                checkName.setTextColor(
                    ContextCompat.getColor(
                        itemView.context,
                        R.color.text_secondary
                    )
                )
                checkName.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_checklist, 0, 0, 0)
            }
        }
    }

    inner class HeaderItemViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val headerName = itemView.findViewById<TextView>(R.id.checkHeader)
        fun bind(header: CheckListItem.Header) {
            headerName.text = header.name
        }
    }

}
