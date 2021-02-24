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
import org.rfcx.companion.entity.Screen
import org.rfcx.companion.util.Analytics
import org.rfcx.companion.view.deployment.EdgeDeploymentProtocol

class NewSyncFragment : Fragment() {
    private var edgeDeploymentProtocol: EdgeDeploymentProtocol? = null
    private lateinit var switchAnimation: AnimationDrawable
    private lateinit var flashingRedAnimation: AnimationDrawable
    private val analytics by lazy { context?.let { Analytics(it) } }
    private var step: Int? = null

    override fun onAttach(context: Context) {
        super.onAttach(context)
        edgeDeploymentProtocol = (context as EdgeDeploymentProtocol)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        arguments?.let { step = it.getInt(STEP) }
        return inflater.inflate(R.layout.fragment_new_sync, container, false)
    }

    override fun onDetach() {
        super.onDetach()
        edgeDeploymentProtocol?.stopPlaySound()
    }

    override fun onResume() {
        super.onResume()
        analytics?.trackScreen(Screen.SYNC)
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

        if(step != null){
            val stepSync = step
            stepSync?.let { setStep(it) }
        } else {
            setStep(1)
        }

        beginSyncButton.setOnClickListener {
            analytics?.trackPlayToneEvent()
            edgeDeploymentProtocol?.playTone(100000)
            setStep(2)
        }
        notHearButton.setOnClickListener {
            analytics?.trackRetryPlayToneEvent()
            edgeDeploymentProtocol?.stopPlaySound()
            setStep(1)
        }
        hearButton.setOnClickListener {
            setStep(3)
        }
        notSwitchButton.setOnClickListener {
            analytics?.trackRetryPlayToneEvent()
            edgeDeploymentProtocol?.stopPlaySound()
            setStep(1)
        }
        switchButton.setOnClickListener {
            setStep(4)
        }
        notSeeLightsAudiomothButton.setOnClickListener {
            analytics?.trackRetryPlayToneEvent()
            edgeDeploymentProtocol?.stopPlaySound()
            setStep(1)
        }
        seeLightsAudiomothButton.setOnClickListener {
            analytics?.trackPlayToneCompletedEvent()
            edgeDeploymentProtocol?.stopPlaySound()
            setStep(5)
        }
        syncAudioMothButton.setOnClickListener {
            movePhoneNearTextView.text = getString(R.string.keep_phone_near)
            syncAudioMothButton.isEnabled = false
            analytics?.trackPlaySyncToneEvent()
            edgeDeploymentProtocol?.playSyncSound()
        }
        notConfirmLightButton.setOnClickListener {
            analytics?.trackRetryPlayToneEvent()
            edgeDeploymentProtocol?.stopPlaySound()
            setStep(1)
        }
        confirmLightButton.setOnClickListener {
            analytics?.trackPlaySyncToneCompletedEvent()
            edgeDeploymentProtocol?.stopPlaySound()
            edgeDeploymentProtocol?.nextStep()
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
                movePhoneNearLayout.visibility = View.GONE
                finishMovePhoneNearLayout.visibility = View.GONE
                confirmLightLayout.visibility = View.GONE
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
                movePhoneNearLayout.visibility = View.GONE
                finishMovePhoneNearLayout.visibility = View.GONE
                confirmLightLayout.visibility = View.GONE
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
                movePhoneNearLayout.visibility = View.GONE
                finishMovePhoneNearLayout.visibility = View.GONE
                confirmLightLayout.visibility = View.GONE
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
                movePhoneNearLayout.visibility = View.GONE
                finishMovePhoneNearLayout.visibility = View.GONE
                confirmLightLayout.visibility = View.GONE
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
                movePhoneNearLayout.visibility = View.VISIBLE
                finishMovePhoneNearLayout.visibility = View.GONE
                confirmLightLayout.visibility = View.GONE
            }
            6 -> {
                setHardwareSwitchToOffLayout.visibility = View.GONE
                finishSetHardwareLayout.visibility = View.VISIBLE
                hearAudioToneLayout.visibility = View.GONE
                finishHearAudioToneLayout.visibility = View.VISIBLE
                switchToCustomLayout.visibility = View.GONE
                finishSwitchToCustomLayout.visibility = View.VISIBLE
                lightsAudiomothLayout.visibility = View.GONE
                finishLightsAudiomothLayout.visibility = View.VISIBLE
                movePhoneNearLayout.visibility = View.GONE
                finishMovePhoneNearLayout.visibility = View.VISIBLE
                confirmLightLayout.visibility = View.VISIBLE
            }
        }
    }

    companion object {
        const val STEP = "STEP"

        @JvmStatic
        fun newInstance() = NewSyncFragment()

        fun newInstance(step: Int) = NewSyncFragment().apply {
            arguments = Bundle().apply {
                putInt(STEP, step)
            }
        }
    }
}
