package org.rfcx.audiomoth.view.deployment.guardian.sync

import android.content.Context
import android.os.Bundle
import android.os.CountDownTimer
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import kotlinx.android.synthetic.main.fragment_guardian_sync.*
import org.rfcx.audiomoth.R
import org.rfcx.audiomoth.connection.socket.OnReceiveResponse
import org.rfcx.audiomoth.connection.socket.SocketManager
import org.rfcx.audiomoth.entity.guardian.GuardianConfiguration
import org.rfcx.audiomoth.entity.guardian.toListForGuardian
import org.rfcx.audiomoth.entity.socket.SocketResposne
import org.rfcx.audiomoth.view.deployment.guardian.GuardianDeploymentProtocol

class GuardianSyncFragment : Fragment() {
    private var deploymentProtocol: GuardianDeploymentProtocol? = null

    private lateinit var countDownTimer: CountDownTimer

    override fun onAttach(context: Context) {
        super.onAttach(context)
        deploymentProtocol = (context as GuardianDeploymentProtocol)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_guardian_sync, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val config = deploymentProtocol?.getConfiguration()
        syncing(config!!)
        retryCountdown()

        retrySyncButton.setOnClickListener {
            hideRetryButton()
            syncing(config)
            retryCountdown()
        }
    }

    private fun syncing(config: GuardianConfiguration) {
        SocketManager.syncConfiguration(config.toListForGuardian(), object : OnReceiveResponse {
            override fun onReceive(response: SocketResposne) {
                activity!!.runOnUiThread {
                    deploymentProtocol?.nextStep()
                    countDownTimer.cancel()
                }
            }

            override fun onFailed(message: String) {
                activity!!.runOnUiThread {
                    Toast.makeText(activity, message, Toast.LENGTH_LONG).show()
                    showRetryButton()
                    countDownTimer.cancel()
                }
            }
        })
    }

    private fun showRetryButton() {
        syncProgress.visibility = View.GONE
        retrySyncButton.visibility = View.VISIBLE
    }

    private fun hideRetryButton() {
        syncProgress.visibility = View.VISIBLE
        retrySyncButton.visibility = View.GONE
    }

    private fun retryCountdown() {
        countDownTimer = object : CountDownTimer(10000, 1000) {
            override fun onFinish() {
                showRetryButton()
                Toast.makeText(activity!!, "Sync failed", Toast.LENGTH_LONG).show()
            }

            override fun onTick(millisUntilFinished: Long) {}
        }
        countDownTimer.start()
    }

    override fun onDetach() {
        super.onDetach()
        countDownTimer.cancel()
    }

    companion object {
        fun newInstance(page: String): GuardianSyncFragment {
            return GuardianSyncFragment()
        }
    }
}
