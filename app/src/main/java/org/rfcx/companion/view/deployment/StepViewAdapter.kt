package org.rfcx.companion.view.deployment

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import org.rfcx.companion.R
import org.rfcx.companion.adapter.StepViewItem

class StepViewAdapter(private val onStepClickListener: (Int) -> Unit) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {

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
        getStepItemFromIndex(position * 2).isPassed = true
        setDividerPasses()
        notifyDataSetChanged()
    }

    private fun setDividerPasses() {
        for (i in 0..listOfSteps.size / 4 + 1) {
            if (getStepItemFromIndex(i * 2).isPassed && getStepItemFromIndex(i * 2 + 2).isPassed) {
                getDividerFromIndex(i * 2 + 1).isPassed = true
            }
        }
    }

    private fun getStepItemFromIndex(index: Int): StepViewItem.StepItem {
        return listOfSteps[index] as StepViewItem.StepItem
    }

    private fun getDividerFromIndex(index: Int): StepViewItem.DividerItem {
        return listOfSteps[index] as StepViewItem.DividerItem
    }

    fun isEveryStepsPassed(): Boolean {
        return listOfSteps.filterIsInstance<StepViewItem.StepItem>().filter { !it.canSkip }.all { it.isPassed }
    }

    fun setStepsCanSkip(steps: List<String>) {
        steps.forEach { step ->
            val skipStep = listOfSteps.filterIsInstance<StepViewItem.StepItem>()
                .find { it.name == step }
            skipStep?.let { it.canSkip = true }
        }
    }

    fun setStepSelected(position: Int) {
        getStepItemFromIndex(position * 2).isSelected = true
        notifyDataSetChanged()
    }

    fun setStepUnSelected(position: Int) {
        getStepItemFromIndex(position * 2).isSelected = false
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
            STEP_ITEM -> StepItemViewHolder(
                LayoutInflater.from(parent.context).inflate(R.layout.item_step, parent, false)
            )
            else -> DividerItemViewHolder(
                LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_step_divider, parent, false)
            )
        }
    }

    override fun getItemCount(): Int = listOfSteps.size

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder.itemViewType) {
            STEP_ITEM -> (holder as StepItemViewHolder).bind(getStepItemFromIndex(position))
            else -> (holder as DividerItemViewHolder).bind(getDividerFromIndex(position))
        }
    }

    inner class StepItemViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val stepNumber = itemView.findViewById<TextView>(R.id.stepNumber)
        private val stepName = itemView.findViewById<TextView>(R.id.stepName)
        private val stepCircle = itemView.findViewById<View>(R.id.stepCircle)
        fun bind(step: StepViewItem.StepItem) {
            stepName.text = step.name

            itemView.setOnClickListener {
                onStepClickListener(step.number)
            }

            if (step.isSelected && step.isPassed) {
                stepNumber.setTextColor(ContextCompat.getColor(itemView.context, R.color.white))
                stepNumber.text = step.number.toString()
                stepName.setTextColor(
                    ContextCompat.getColor(
                        itemView.context,
                        R.color.colorPrimary
                    )
                )
                stepCircle.background =
                    ContextCompat.getDrawable(itemView.context, R.drawable.circle_step_passed)
                itemView.isEnabled = true
            } else if (step.isSelected) {
                stepNumber.setTextColor(ContextCompat.getColor(itemView.context, R.color.white))
                stepNumber.text = step.number.toString()
                stepName.setTextColor(
                    ContextCompat.getColor(
                        itemView.context,
                        R.color.colorPrimary
                    )
                )
                stepCircle.background =
                    ContextCompat.getDrawable(itemView.context, R.drawable.circle_step_passed)
                itemView.isEnabled = true
            } else if (step.isPassed) {
                stepNumber.setTextColor(ContextCompat.getColor(itemView.context, R.color.white))
                stepNumber.text = itemView.context.getString(R.string.correct_mark)
                stepName.setTextColor(
                    ContextCompat.getColor(
                        itemView.context,
                        R.color.text_secondary
                    )
                )
                stepCircle.background =
                    ContextCompat.getDrawable(itemView.context, R.drawable.circle_step_passed)
                itemView.isEnabled = true
            } else {
                stepNumber.setTextColor(
                    ContextCompat.getColor(
                        itemView.context,
                        R.color.text_secondary
                    )
                )
                stepNumber.text = step.number.toString()
                stepName.setTextColor(
                    ContextCompat.getColor(
                        itemView.context,
                        R.color.text_secondary
                    )
                )
                stepCircle.background =
                    ContextCompat.getDrawable(itemView.context, R.drawable.circle_step_not_passed)
                itemView.isEnabled = true
            }
        }
    }

    inner class DividerItemViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val dividerLine = itemView.findViewById<View>(R.id.stepDivider)
        fun bind(divider: StepViewItem.DividerItem) {
            if (divider.isPassed) {
                dividerLine.setBackgroundColor(
                    ContextCompat.getColor(
                        itemView.context,
                        R.color.colorPrimary
                    )
                )
            } else {
                dividerLine.setBackgroundColor(
                    ContextCompat.getColor(
                        itemView.context,
                        R.color.text_secondary
                    )
                )
            }
        }
    }
}
