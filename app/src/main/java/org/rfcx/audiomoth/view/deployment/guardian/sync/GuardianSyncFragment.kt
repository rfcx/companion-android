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
import org.rfcx.audiomoth.connection.socket.OnReceiveResponse
import org.rfcx.audiomoth.connection.socket.SocketManager
import org.rfcx.audiomoth.entity.guardian.GuardianConfiguration
import org.rfcx.audiomoth.entity.socket.SocketResposne
import org.rfcx.audiomoth.view.deployment.guardian.GuardianDeploymentProtocol

class GuardianSyncFragment : Fragment() {
    private var deploymentProtocol: GuardianDeploymentProtocol? = null

    override fun onAttach(context: Context) {
        super.onAttach(context)
        deploymentProtocol = (context as GuardianDeploymentProtocol)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_guardian_sync, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val config = deploymentProtocol?.getConfiguration()
        syncing(config!!)
    }

    private fun syncing(config: GuardianConfiguration) {
        SocketManager.syncConfiguration(config, object : OnReceiveResponse{
            override fun onReceive(response: SocketResposne) {
                deploymentProtocol?.nextStep()
            }

            override fun onFailed() {
                TODO("Not yet implemented")
            }
        })
    }

    companion object {
        fun newInstance(page: String) : GuardianSyncFragment {
            return GuardianSyncFragment()
        }
    }
}
