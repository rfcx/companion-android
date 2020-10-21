package org.rfcx.companion.view.deployment.sync

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import kotlinx.android.synthetic.main.fragment_after_play_initial_tone.*
import kotlinx.android.synthetic.main.fragment_after_play_initial_tone.noButton
import kotlinx.android.synthetic.main.fragment_after_play_initial_tone.yesButton
import kotlinx.android.synthetic.main.fragment_after_play_sync_tone.*
import kotlinx.android.synthetic.main.fragment_initial_tone_playing.*
import kotlinx.android.synthetic.main.fragment_play_sync_tone.*
import kotlinx.android.synthetic.main.fragment_start_sync_process.*
import org.rfcx.companion.R
import org.rfcx.companion.view.deployment.EdgeDeploymentProtocol

class SyncFragment : Fragment() {
    private var edgeDeploymentProtocol: EdgeDeploymentProtocol? = null
    private var status: String? = null

    override fun onAttach(context: Context) {
        super.onAttach(context)
        edgeDeploymentProtocol = (context as EdgeDeploymentProtocol)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        var view = inflater.inflate(R.layout.fragment_start_sync_process, container, false)
        arguments?.let { status = it.getString(STATUS) }

        when (status) {
            INITIAL_TONE_PLAYING -> view =
                inflater.inflate(R.layout.fragment_initial_tone_playing, container, false)
            AFTER_PLAY_INITIAL_TONE -> view =
                inflater.inflate(R.layout.fragment_after_play_initial_tone, container, false)
            PLAY_SYNC_TONE -> view =
                inflater.inflate(R.layout.fragment_play_sync_tone, container, false)
            AFTER_PLAY_SYNC_TONE -> view =
                inflater.inflate(R.layout.fragment_after_play_sync_tone, container, false)
        }
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        edgeDeploymentProtocol?.let {
            it.showToolbar()
            it.setCurrentPage(requireContext().resources.getStringArray(R.array.edge_setup_checks)[1])
            it.setToolbarTitle()
        }

        when (status) {
            START_SYNC -> startSyncProcess()
            INITIAL_TONE_PLAYING -> initialTonePlaying()
            AFTER_PLAY_INITIAL_TONE -> afterPlayInitialTone()
            PLAY_SYNC_TONE -> playSyncTone()
            AFTER_PLAY_SYNC_TONE -> afterPlaySyncTone()
        }
    }

    private fun startSyncProcess() {
        playToneButton.setOnClickListener {
            edgeDeploymentProtocol?.playTone()
        }
    }

    private fun initialTonePlaying() {
        Glide.with(this).load(R.drawable.audiomoth_switch).into(audioMothSwitchImageView)
        nextButton.setOnClickListener {
            edgeDeploymentProtocol?.startSyncing(AFTER_PLAY_INITIAL_TONE)
        }
    }

    private fun afterPlayInitialTone() {
        Glide.with(this).load(R.drawable.audiomoth_flashing_green)
            .into(audioMothFlashingGreenImageView)

        yesButton.setOnClickListener {
            edgeDeploymentProtocol?.startSyncing(PLAY_SYNC_TONE)
        }

        noButton.setOnClickListener {
            edgeDeploymentProtocol?.startSyncing(START_SYNC)
        }
    }

    private fun playSyncTone() {
        playSyncToneButton.isSoundEffectsEnabled = false
        playSyncToneButton.setOnClickListener {
            edgeDeploymentProtocol?.playSyncSound()
            edgeDeploymentProtocol?.startSyncing(AFTER_PLAY_SYNC_TONE)
        }
    }

    private fun afterPlaySyncTone() {
        Glide.with(this).load(R.drawable.audiomoth_flashing_red)
            .into(audioMothFlashingRedImageView)
        yesButton.setOnClickListener {
            edgeDeploymentProtocol?.nextStep()
        }

        noButton.setOnClickListener {
            edgeDeploymentProtocol?.startSyncing(START_SYNC)
        }
    }

    companion object {
        const val STATUS = "STATUS"
        const val START_SYNC = "START_SYNC"
        const val INITIAL_TONE_PLAYING = "INITIAL_TONE_PLAYING"
        const val AFTER_PLAY_INITIAL_TONE = "AFTER_PLAY_INITIAL_TONE"
        const val PLAY_SYNC_TONE = "PLAY_SYNC_TONE"
        const val AFTER_PLAY_SYNC_TONE = "AFTER_PLAY_SYNC_TONE"

        @JvmStatic
        fun newInstance(page: String) = SyncFragment()
            .apply {
                arguments = Bundle().apply {
                    putString(STATUS, page)
                }
            }
    }
}
