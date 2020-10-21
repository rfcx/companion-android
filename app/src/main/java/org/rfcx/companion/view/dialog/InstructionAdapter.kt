package org.rfcx.companion.view.dialog

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.item_instruction.view.*
import org.rfcx.companion.R
import org.rfcx.companion.adapter.InstructionItem

class InstructionAdapter : RecyclerView.Adapter<InstructionAdapter.InstructionViewHolder>() {

    private var listOfInstruction = arrayListOf<InstructionItem>()

    fun setInstructions(instructions: List<String>) {
        instructions.forEachIndexed { number, desc ->
            listOfInstruction.add(InstructionItem(number + 1, desc))
        }
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): InstructionViewHolder {
        return InstructionViewHolder(
            LayoutInflater.from(parent.context)
                .inflate(R.layout.item_instruction, parent, false)
        )
    }

    override fun getItemCount(): Int = listOfInstruction.size

    override fun onBindViewHolder(holder: InstructionViewHolder, position: Int) {
        holder.bind(listOfInstruction[position])
    }

    inner class InstructionViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        private val instructionNumber = itemView.instructionNumberText
        private val instructionDesc = itemView.instructionDescText

        fun bind(instruction: InstructionItem) {
            instructionNumber.text = instruction.number.toString()
            instructionDesc.text = instruction.description
        }
    }
}
