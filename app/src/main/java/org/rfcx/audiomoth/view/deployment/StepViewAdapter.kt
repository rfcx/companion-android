package org.rfcx.audiomoth.view.deployment

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import org.rfcx.audiomoth.R
import org.rfcx.audiomoth.adapter.StepViewItem

class StepViewAdapter(private val onStepClickListener: (Int) -> Unit) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        const val STEP_ITEM = 1
        const val DIVIDER_ITEM = 2
    }

    private val listOfSteps = arrayListOf<StepViewItem>()

    fun setSteps(steps: List<String>) {
        steps.forEachIndexed { index, step ->
            listOfSteps.add(StepViewItem.StepItem(index + 1, step))
            if (index != steps.size - 1) {
                listOfSteps.add(StepViewItem.DividerItem(index + 1))
            }
        }
    }

    fun setStepPasses(position: Int) {
        (listOfSteps[position * 2] as StepViewItem.StepItem).isPassed = true
        if (position != 0) {
            (listOfSteps[position * 2 - 1] as StepViewItem.DividerItem).isPassed = true
        }
        notifyDataSetChanged()
    }

    override fun getItemViewType(position: Int): Int {
        return when (listOfSteps[position]) {
            is StepViewItem.StepItem -> STEP_ITEM
            else -> DIVIDER_ITEM
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            STEP_ITEM -> StepItemViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_step, parent, false))
            else -> DividerItemViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_step_divider, parent, false))
        }
    }

    override fun getItemCount(): Int = listOfSteps.size

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder.itemViewType) {
            STEP_ITEM -> (holder as StepItemViewHolder).bind(listOfSteps[position] as StepViewItem.StepItem)
            else -> (holder as DividerItemViewHolder).bind(listOfSteps[position] as StepViewItem.DividerItem)
        }
    }

    inner class StepItemViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val stepNumber = itemView.findViewById<TextView>(R.id.stepNumber)
        private val stepName = itemView.findViewById<TextView>(R.id.stepName)
        private val stepCircle = itemView.findViewById<View>(R.id.stepCircle)
        fun bind(step: StepViewItem.StepItem) {
            stepNumber.text = step.number.toString()
            stepName.text = step.name

            itemView.setOnClickListener {
                onStepClickListener(step.number)
            }

            if (step.isPassed) {
                stepNumber.setTextColor(ContextCompat.getColor(itemView.context, R.color.white))
                stepName.setTextColor(ContextCompat.getColor(itemView.context, R.color.colorPrimary))
                stepCircle.background = ContextCompat.getDrawable(itemView.context, R.drawable.circle_step_passed)
                itemView.isEnabled = true
            } else {
                stepNumber.setTextColor(ContextCompat.getColor(itemView.context, R.color.text_secondary))
                stepName.setTextColor(ContextCompat.getColor(itemView.context, R.color.text_secondary))
                stepCircle.background = ContextCompat.getDrawable(itemView.context, R.drawable.circle_step_not_passed)
                itemView.isEnabled = false
            }
        }
    }

    inner class DividerItemViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val dividerLine = itemView.findViewById<View>(R.id.stepDivider)
        fun bind(divider: StepViewItem.DividerItem) {
            if (divider.isPassed) {
                dividerLine.setBackgroundColor(ContextCompat.getColor(itemView.context, R.color.colorPrimary))
            }
        }
    }
}
