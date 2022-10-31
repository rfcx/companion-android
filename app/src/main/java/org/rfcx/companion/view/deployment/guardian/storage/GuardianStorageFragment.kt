package org.rfcx.companion.view.deployment.guardian.storage

import android.os.Bundle
import android.text.format.Formatter
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import kotlinx.android.synthetic.main.fragment_guardian_storage.*
import org.rfcx.companion.R
import org.rfcx.companion.connection.socket.AdminSocketManager
import org.rfcx.companion.entity.Screen
import org.rfcx.companion.util.Analytics
import org.rfcx.companion.view.deployment.guardian.GuardianDeploymentProtocol

class GuardianStorageFragment : Fragment() {

    private val deploymentProtocol by lazy { (context?.let { it as GuardianDeploymentProtocol } ) }

    private val analytics by lazy { context?.let { Analytics(it) } }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_guardian_storage, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        deploymentProtocol?.let {
            it.setToolbarSubtitle()
            it.setMenuToolbar(false)
            it.showToolbar()
            it.setToolbarTitle()
        }

        updateInternalStorage()
    }

    private fun updateInternalStorage() {
        AdminSocketManager.pingBlob.observe(viewLifecycleOwner) {
            val storage = deploymentProtocol?.getStorage()

            storage?.internal?.let { str ->
                val used = str.used
                val all = str.all
                internalSizeTextView.text = getString(R.string.storage_format, Formatter.formatFileSize(requireContext(), used), Formatter.formatFileSize(requireContext(), all))
                internalStorageBar.max = 100
                internalStorageBar.progress = ((used.toFloat() / all.toFloat()) * 100).toInt()
                internalStorageBar.show()
            }

            storage?.external?.let { str ->
                val used = str.used
                val all = str.all
                externalSizeTextView.text = getString(R.string.storage_format, Formatter.formatFileSize(requireContext(), used), Formatter.formatFileSize(requireContext(), all))
                externalStorageBar.max = 100
                externalStorageBar.progress = ((used.toFloat() / all.toFloat()) * 100).toInt()
                externalStorageBar.show()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        analytics?.trackScreen(Screen.GUARDIAN_STORAGE)
    }

    companion object {
        fun newInstance() = GuardianStorageFragment()
    }
}
