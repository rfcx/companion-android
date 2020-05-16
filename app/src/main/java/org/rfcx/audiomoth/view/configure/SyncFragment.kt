package org.rfcx.audiomoth.view.configure


import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import kotlinx.android.synthetic.main.fragment_after_sync.*
import kotlinx.android.synthetic.main.fragment_before_sync.*
import kotlinx.android.synthetic.main.fragment_sync.*
import org.rfcx.audiomoth.R
import org.rfcx.audiomoth.view.DeploymentListener
import org.rfcx.audiomoth.view.DeploymentProtocol

class SyncFragment : Fragment() {

    lateinit var deploymentListener: DeploymentListener
    lateinit var deploymentProtocol: DeploymentProtocol
    private var status: String? = null

    override fun onAttach(context: Context) {
        super.onAttach(context)
        deploymentListener = (context as DeploymentListener)
        deploymentProtocol = (context as DeploymentProtocol)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
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
            SYNCING -> syncing()
            AFTER_SYNC -> afterSync()
        }
    }

    fun beforeSync() {
        nextButton.setOnClickListener {
            deploymentListener.openSync(SYNCING)
        }
    }

    fun syncing() {
        var i = 0
        val handler = Handler()

        handler.postDelayed(object : Runnable {
            override fun run() {
                if (i != 100) {
                    i += 20
                    progressBarHorizontal.progress = i
                    percentSyncTextView.text = "$i %"
                    handler.postDelayed(this, 500)
                } else {
                    deploymentListener.openSync(AFTER_SYNC)
                }
            }
        }, 0)
    }

    fun afterSync() {
        nextAfterSyncButton.setOnClickListener {
            deploymentProtocol.nextStep()
        }
    }

    companion object {
        private const val STATUS = "STATUS"
        private const val SYNCING = "SYNCING"
        private const val AFTER_SYNC = "AFTER_SYNC"
        const val BEFORE_SYNC = "BEFORE_SYNC"

        @JvmStatic
        fun newInstance(page: String) = SyncFragment().apply {
            arguments = Bundle().apply {
                putString(STATUS, page)
            }
        }
    }
}

