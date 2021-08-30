package org.rfcx.companion.view.deployment

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import kotlinx.android.synthetic.main.fragment_choose_device.*
import org.rfcx.companion.R
import org.rfcx.companion.entity.Screen
import org.rfcx.companion.util.Analytics

class ChooseDeviceFragment : Fragment() {
    private var audioMothDeploymentProtocol: AudioMothDeploymentProtocol? = null
    private val analytics by lazy { context?.let { Analytics(it) } }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        audioMothDeploymentProtocol = (context as AudioMothDeploymentProtocol)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        audioMothDeploymentProtocol?.hideToolbar()

        edgeImageView.setOnClickListener {
            audioMothDeploymentProtocol?.openWithEdgeDevice()
        }

        edgeTextView.setOnClickListener {
            audioMothDeploymentProtocol?.openWithEdgeDevice()
        }

        guardianImageView.setOnClickListener {
            audioMothDeploymentProtocol?.openWithGuardianDevice()
        }

        guardianTextview.setOnClickListener {
            audioMothDeploymentProtocol?.openWithGuardianDevice()
        }
    }

    override fun onResume() {
        super.onResume()
        analytics?.trackScreen(Screen.CHOOSE_DEVICE)
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
