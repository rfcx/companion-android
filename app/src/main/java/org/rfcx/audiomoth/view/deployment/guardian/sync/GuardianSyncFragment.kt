package org.rfcx.audiomoth.view.deployment.guardian.sync

import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import kotlinx.android.synthetic.main.fragment_sync.*
import org.rfcx.audiomoth.R
import org.rfcx.audiomoth.view.deployment.guardian.GuardianDeploymentProtocol

class GuardianSyncFragment : Fragment() {
    private var deploymentProtocol: GuardianDeploymentProtocol? = null

    override fun onAttach(context: Context) {
        super.onAttach(context)
        deploymentProtocol = (context as GuardianDeploymentProtocol)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        //TODO: create seperate layout for guardian
        return inflater.inflate(R.layout.fragment_guardian_sync, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        syncing()
    }

    private fun syncing() {
        var i = 0
        val handler = Handler()

        //TODO: implement Socket data transfer here and use response back to continue next step
        val timerRunnable = object : Runnable {
            override fun run() {
                if (i != 100) {
                    i += 20
                    if (progressBarHorizontal != null && percentSyncTextView != null) {
                        progressBarHorizontal.progress = i
                        percentSyncTextView.text = "$i %"
                    }
                    handler.postDelayed(this, 500)
                } else {
                    deploymentProtocol?.nextStep()
                }
            }
        }
        handler.postDelayed(timerRunnable, 0)
    }

    companion object {
        fun newInstance(page: String) : GuardianSyncFragment {
            return GuardianSyncFragment()
        }
    }
}