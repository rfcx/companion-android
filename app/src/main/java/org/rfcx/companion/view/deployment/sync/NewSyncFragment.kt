package org.rfcx.companion.view.deployment.sync

import android.content.Context
import android.graphics.drawable.AnimationDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.fragment.app.Fragment
import kotlinx.android.synthetic.main.fragment_new_sync.*
import org.rfcx.companion.R
import org.rfcx.companion.view.deployment.EdgeDeploymentProtocol

class NewSyncFragment : Fragment() {
    private var edgeDeploymentProtocol: EdgeDeploymentProtocol? = null
    private lateinit var switchAnimation: AnimationDrawable
    private lateinit var flashingRedAnimation: AnimationDrawable

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

        view.findViewById<ImageView>(R.id.switchCustomImageView).apply {
            setBackgroundResource(R.drawable.audiomoth_switch_to_custom)
            switchAnimation = background as AnimationDrawable
        }
        switchAnimation.start()

        view.findViewById<ImageView>(R.id.lightsAudiomothImageView).apply {
            setBackgroundResource(R.drawable.audiomoth_green_flashing)
            flashingRedAnimation = background as AnimationDrawable
        }
        flashingRedAnimation.start()

        setStep(1)

        beginSyncButton.setOnClickListener {
            setStep(2)
        }
        notHearButton.setOnClickListener {
            setStep(1)
        }
        hearButton.setOnClickListener {
            setStep(3)
        }
        notSwitchButton.setOnClickListener {
            setStep(2)
        }
        switchButton.setOnClickListener {
            setStep(4)
        }
        notSeeLightsAudiomothButton.setOnClickListener {
            setStep(3)
        }
        seeLightsAudiomothButton.setOnClickListener {
            setStep(5)
        }
    }

    private fun setStep(step: Int) {
        when (step) {
            1 -> {
                setHardwareSwitchToOffLayout.visibility = View.VISIBLE
                finishSetHardwareLayout.visibility = View.GONE
                hearAudioToneLayout.visibility = View.GONE
                finishHearAudioToneLayout.visibility = View.GONE
                switchToCustomLayout.visibility = View.GONE
                finishSwitchToCustomLayout.visibility = View.GONE
                lightsAudiomothLayout.visibility = View.GONE
                finishLightsAudiomothLayout.visibility = View.GONE
            }
            2 -> {
                setHardwareSwitchToOffLayout.visibility = View.GONE
                finishSetHardwareLayout.visibility = View.VISIBLE
                hearAudioToneLayout.visibility = View.VISIBLE
                finishHearAudioToneLayout.visibility = View.GONE
                switchToCustomLayout.visibility = View.GONE
                finishSwitchToCustomLayout.visibility = View.GONE
                lightsAudiomothLayout.visibility = View.GONE
                finishLightsAudiomothLayout.visibility = View.GONE
            }
            3 -> {
                setHardwareSwitchToOffLayout.visibility = View.GONE
                finishSetHardwareLayout.visibility = View.VISIBLE
                hearAudioToneLayout.visibility = View.GONE
                finishHearAudioToneLayout.visibility = View.VISIBLE
                switchToCustomLayout.visibility = View.VISIBLE
                finishSwitchToCustomLayout.visibility = View.GONE
                lightsAudiomothLayout.visibility = View.GONE
                finishLightsAudiomothLayout.visibility = View.GONE
            }
            4 -> {
                setHardwareSwitchToOffLayout.visibility = View.GONE
                finishSetHardwareLayout.visibility = View.VISIBLE
                hearAudioToneLayout.visibility = View.GONE
                finishHearAudioToneLayout.visibility = View.VISIBLE
                switchToCustomLayout.visibility = View.GONE
                finishSwitchToCustomLayout.visibility = View.VISIBLE
                lightsAudiomothLayout.visibility = View.VISIBLE
                finishLightsAudiomothLayout.visibility = View.GONE
            }
            5 -> {
                setHardwareSwitchToOffLayout.visibility = View.GONE
                finishSetHardwareLayout.visibility = View.VISIBLE
                hearAudioToneLayout.visibility = View.GONE
                finishHearAudioToneLayout.visibility = View.VISIBLE
                switchToCustomLayout.visibility = View.GONE
                finishSwitchToCustomLayout.visibility = View.VISIBLE
                lightsAudiomothLayout.visibility = View.GONE
                finishLightsAudiomothLayout.visibility = View.VISIBLE
            }
        }
    }

    companion object {
        @JvmStatic
        fun newInstance() = NewSyncFragment()
    }
}
