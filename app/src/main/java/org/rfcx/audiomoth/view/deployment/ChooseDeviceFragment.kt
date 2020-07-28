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
    private var deploymentProtocol: DeploymentProtocol? = null

    override fun onAttach(context: Context) {
        super.onAttach(context)
        deploymentProtocol = (context as DeploymentProtocol)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        deploymentProtocol?.hideCompleteButton()
        deploymentProtocol?.hideStepView()

        edgeImageView.setOnClickListener {
            deploymentProtocol?.openWithEdgeDevice()
        }

        edgeTextView.setOnClickListener {
            deploymentProtocol?.openWithEdgeDevice()
        }

        guardianImageView.setOnClickListener {
            deploymentProtocol?.openWithGuardianDevice()
        }

        guardianTextview.setOnClickListener {
            deploymentProtocol?.openWithGuardianDevice()
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
