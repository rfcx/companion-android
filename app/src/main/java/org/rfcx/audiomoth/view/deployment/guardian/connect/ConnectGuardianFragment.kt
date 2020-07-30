package org.rfcx.audiomoth.view.deployment.guardian.connect

import android.content.Context
import android.net.wifi.ScanResult
import android.os.Bundle
import android.os.CountDownTimer
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.android.synthetic.main.fragment_connect_guardian.*
import org.rfcx.audiomoth.R
import org.rfcx.audiomoth.connection.socket.SocketManager
import org.rfcx.audiomoth.connection.wifi.OnWifiListener
import org.rfcx.audiomoth.connection.wifi.WifiHotspotManager
import org.rfcx.audiomoth.entity.socket.Status
import org.rfcx.audiomoth.view.deployment.guardian.GuardianDeploymentProtocol

class ConnectGuardianFragment : Fragment(), OnWifiListener, (ScanResult) -> Unit {
    private val guardianHotspotAdapter by lazy { GuardianHotspotAdapter(this) }
    private var deploymentProtocol: GuardianDeploymentProtocol? = null
    private lateinit var wifiHotspotManager: WifiHotspotManager

    private var guardianHotspot: ScanResult? = null

    private lateinit var countDownTimer: CountDownTimer

    private var connectionCount = 0

    override fun onAttach(context: Context) {
        super.onAttach(context)
        deploymentProtocol = (context as GuardianDeploymentProtocol)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_connect_guardian, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        deploymentProtocol?.hideCompleteButton()
        showLoading()
        retryCountdown(SCAN)

        guardianHotspotRecyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = guardianHotspotAdapter
        }

        wifiHotspotManager = WifiHotspotManager(requireContext().applicationContext)
        wifiHotspotManager.nearbyHotspot(this)

        connectGuardianButton.setOnClickListener {
            showLoading()
            retryCountdown(CONNECT)
            wifiHotspotManager.connectTo(guardianHotspot!!, this)
        }

        retryGuardianButton.setOnClickListener {
            showLoading()
            hideRetry()
            hideNotFound()
            retryCountdown(SCAN)
            wifiHotspotManager.nearbyHotspot(this)
        }
    }

    override fun invoke(hotspot: ScanResult) {
        guardianHotspot = hotspot
        enableConnectButton()
    }

    override fun onScanReceive(result: List<ScanResult>) {
        hideLoading()
        hideNotFound()
        hideRetry()
        wifiHotspotManager.unRegisterReceiver()
        countDownTimer.cancel()
        guardianHotspotAdapter.items = result
    }

    override fun onWifiConnected() {
        // connect to SocketServer
        SocketManager.getConnection()
        SocketManager.connection.observe(viewLifecycleOwner, Observer { response ->
            requireActivity().runOnUiThread {
                Log.d("Socket", "data changed")
                if (response.connection.status == Status.SUCCESS.value) {
                    if (connectionCount == 0) {
                        deploymentProtocol?.setDeploymentWifiName(guardianHotspot!!.SSID)
                        deploymentProtocol?.nextStep()
                    }
                    connectionCount += 1
                } else {
                    hideLoading()
                }
            }
        })
    }

    private fun showLoading() {
        connectGuardianLoading?.visibility = View.VISIBLE
        connectGuardianButton.visibility = View.GONE
    }

    private fun hideLoading() {
        connectGuardianLoading.visibility = View.GONE
        connectGuardianButton.visibility = View.VISIBLE
    }

    private fun showRetry() {
        retryGuardianButton.visibility = View.VISIBLE
        connectGuardianLoading.visibility = View.GONE
        connectGuardianButton.visibility = View.GONE
    }

    private fun hideRetry() {
        retryGuardianButton.visibility = View.GONE
    }

    private fun showNotFound() {
        notFoundTextView.visibility = View.VISIBLE
    }

    private fun hideNotFound() {
        notFoundTextView.visibility = View.GONE
    }

    private fun retryCountdown(state: String) {
        countDownTimer = object : CountDownTimer(15000, 1000) {
            override fun onFinish() {
                if (state == SCAN) {
                    showNotFound()
                    showRetry()
                    wifiHotspotManager.unRegisterReceiver()
                } else {
                    hideLoading()
                    Toast.makeText(activity!!, "Connection failed", Toast.LENGTH_LONG).show()
                }
            }

            override fun onTick(millisUntilFinished: Long) {}
        }
        countDownTimer.start()
    }

    private fun enableConnectButton() {
        connectGuardianButton.isEnabled = true
    }

    override fun onDetach() {
        super.onDetach()
        wifiHotspotManager.unRegisterReceiver()
        countDownTimer.cancel()
    }

    companion object {
        private val SCAN = "scan"
        private val CONNECT = "connect"

        fun newInstance(): ConnectGuardianFragment {
            return ConnectGuardianFragment()
        }
    }
}
