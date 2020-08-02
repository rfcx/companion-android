package org.rfcx.audiomoth.view.deployment.sync

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import kotlinx.android.synthetic.main.fragment_after_sync.*
import kotlinx.android.synthetic.main.fragment_before_sync.*
import org.rfcx.audiomoth.R
import org.rfcx.audiomoth.view.deployment.EdgeDeploymentProtocol

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
        var view = inflater.inflate(R.layout.fragment_before_sync, container, false)
        arguments?.let { status = it.getString(STATUS) }

        when (status) {
            SYNCING -> view = inflater.inflate(R.layout.fragment_sync, container, false)
            AFTER_SYNC -> view = inflater.inflate(R.layout.fragment_after_sync, container, false)
        }
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        when (status) {
            BEFORE_SYNC -> beforeSync()
            AFTER_SYNC -> afterSync()
        }
    }

    private fun beforeSync() {
        edgeDeploymentProtocol?.hideCompleteButton()
        nextButton.isSoundEffectsEnabled = false

        nextButton.setOnClickListener {
            edgeDeploymentProtocol?.playSyncSound()
            edgeDeploymentProtocol?.startSyncing(SYNCING)
        }
    }

    private fun afterSync() {
        yesButton.setOnClickListener {
            edgeDeploymentProtocol?.nextStep()
        }

        noButton.setOnClickListener {
            edgeDeploymentProtocol?.startSyncing(BEFORE_SYNC)
        }
    }

    companion object {
        const val STATUS = "STATUS"
        const val SYNCING = "SYNCING"
        const val AFTER_SYNC = "AFTER_SYNC"
        const val BEFORE_SYNC = "BEFORE_SYNC"

        @JvmStatic
        fun newInstance(page: String) = SyncFragment()
            .apply {
            arguments = Bundle().apply {
                putString(STATUS, page)
            }
        }
    }
}
