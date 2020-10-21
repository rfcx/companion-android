package org.rfcx.audiomoth.view.deployment.sync

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import kotlinx.android.synthetic.main.fragment_after_sync.*
import kotlinx.android.synthetic.main.fragment_initial_tone_playing.*
import kotlinx.android.synthetic.main.fragment_start_sync_process.*
import org.rfcx.audiomoth.R
import org.rfcx.audiomoth.view.deployment.EdgeDeploymentProtocol

class SyncFragment : Fragment() {
    private var edgeDeploymentProtocol: EdgeDeploymentProtocol? = null
    private var status: String? = null

    override fun onAttach(context: Context) {
        super.onAttach(context)
        edgeDeploymentProtocol = (context as EdgeDeploymentProtocol)
    }

//    start_sync_process
//    initial_tone_playing_process
//    after_play_initial_tone_process
//    play_sync_tone
//    after_play_sync_tone

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
            AFTER_PLAY_INITIAL_TONE -> view = inflater.inflate(R.layout.fragment_after_sync, container, false)
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
            AFTER_PLAY_INITIAL_TONE -> afterSync()
        }
    }

    private fun startSyncProcess() {
//        nextButton.isSoundEffectsEnabled = false
//
//        nextButton.setOnClickListener {
//            edgeDeploymentProtocol?.playSyncSound()
//            edgeDeploymentProtocol?.startSyncing(SYNCING)
//        }

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

    private fun afterSync() {
        yesButton.setOnClickListener {
            edgeDeploymentProtocol?.nextStep()
        }

        noButton.setOnClickListener {
//            edgeDeploymentProtocol?.startSyncing(BEFORE_SYNC)
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
