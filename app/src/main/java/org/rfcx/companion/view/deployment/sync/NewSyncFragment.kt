package org.rfcx.companion.view.deployment.sync

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import kotlinx.android.synthetic.main.fragment_new_sync.*
import org.rfcx.companion.R
import org.rfcx.companion.view.deployment.EdgeDeploymentProtocol

class NewSyncFragment : Fragment() {
    private var edgeDeploymentProtocol: EdgeDeploymentProtocol? = null

    override fun onAttach(context: Context) {
        super.onAttach(context)
        edgeDeploymentProtocol = (context as EdgeDeploymentProtocol)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_new_sync, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        edgeDeploymentProtocol?.let {
            it.showToolbar()
            it.setCurrentPage(requireContext().resources.getStringArray(R.array.edge_setup_checks)[1])
            it.setToolbarTitle()
        }

        beginSyncButton.setOnClickListener {
            setStep(1)
        }
    }

    private fun setStep(step: Int) {
        when(step) {
            0 -> {
                finishSetHardwareLayout.visibility = View.GONE
                setHardwareSwitchToOffLayout.visibility = View.VISIBLE
            }
            1 -> {
                finishSetHardwareLayout.visibility = View.VISIBLE
                setHardwareSwitchToOffLayout.visibility = View.GONE
            }
        }
    }

    companion object {
        @JvmStatic
        fun newInstance() = NewSyncFragment()
    }
}
