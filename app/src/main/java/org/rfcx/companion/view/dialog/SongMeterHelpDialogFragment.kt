package org.rfcx.companion.view.dialog

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.android.synthetic.main.fragment_instruction_dialog.*
import org.rfcx.companion.R

class SongMeterHelpDialogFragment : DialogFragment() {

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

        instructionImage.setBackgroundResource(R.drawable.songmeter_buttons)

        instructionButton.setOnClickListener {
            dismissDialog()
        }

        moreInfoButton.setOnClickListener {
            val uriUrl = Uri.parse("https://www.wildlifeacoustics.com/uploads/user-guides/Song-Meter-Micro-User-Guide.pdf")
            val launchBrowser = Intent(Intent.ACTION_VIEW, uriUrl)
            startActivity(launchBrowser)
        }
    }

    private fun getInstruction(context: Context): List<String> {
        return context.resources.getStringArray(R.array.songmeter_connect_instructions).toList()
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
