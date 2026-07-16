package org.rfcx.companion.view.dialog

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import org.rfcx.companion.databinding.FragmentSitesLoadingBinding

class SiteLoadingDialogFragment(private val text: String) : DialogFragment() {

    private var _binding: FragmentSitesLoadingBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentSitesLoadingBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        dialog?.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        binding.siteLoadingText.text = this.text

        binding.siteLoadingButton.setOnClickListener {
            dismissDialog()
        }
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
        fun newInstance(text: String) = SiteLoadingDialogFragment(text)
    }
}
