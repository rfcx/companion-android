package org.rfcx.companion.view.dialog

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.android.synthetic.main.fragment_instruction_dialog.*
import org.rfcx.companion.R

class ConnectInstructionDialogFragment : DialogFragment() {

    private val instructionAdapter by lazy { InstructionAdapter() }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_instruction_dialog, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        dialog?.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        instructionView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = instructionAdapter
        }
        instructionAdapter.setInstructions(getInstruction(requireContext()))

        instructionButton.setOnClickListener {
            dismissDialog()
        }
    }

    private fun getInstruction(context: Context): List<String> {
        return context.resources.getStringArray(R.array.guardian_connect_instructions).toList()
    }

    private fun dismissDialog() {
        try {
            dismiss()
        } catch (e: Exception) {
            e.printStackTrace()
            dismissAllowingStateLoss()
        }
    }

    companion object {
        fun newInstance() = ConnectInstructionDialogFragment()
    }
}
