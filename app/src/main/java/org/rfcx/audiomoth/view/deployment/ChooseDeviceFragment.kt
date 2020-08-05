package org.rfcx.audiomoth.view.deployment

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import kotlinx.android.synthetic.main.fragment_choose_device.*
import org.rfcx.audiomoth.R

class ChooseDeviceFragment : Fragment() {
    private var edgeDeploymentProtocol: EdgeDeploymentProtocol? = null

    override fun onAttach(context: Context) {
        super.onAttach(context)
        edgeDeploymentProtocol = (context as EdgeDeploymentProtocol)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        edgeDeploymentProtocol?.hideCompleteButton()
        edgeDeploymentProtocol?.hideStepView()

        edgeImageView.setOnClickListener {
            edgeDeploymentProtocol?.openWithEdgeDevice()
        }

        edgeTextView.setOnClickListener {
            edgeDeploymentProtocol?.openWithEdgeDevice()
        }

        guardianImageView.setOnClickListener {
            edgeDeploymentProtocol?.openWithGuardianDevice()
        }

        guardianTextview.setOnClickListener {
            edgeDeploymentProtocol?.openWithGuardianDevice()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_choose_device, container, false)
    }

    companion object {
        fun newInstance() = ChooseDeviceFragment()
    }
}
