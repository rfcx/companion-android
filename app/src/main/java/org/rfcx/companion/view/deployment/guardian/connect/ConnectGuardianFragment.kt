package org.rfcx.companion.view.deployment.guardian.connect

import android.content.Context
import android.net.wifi.ScanResult
import android.os.Bundle
import android.os.CountDownTimer
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.android.synthetic.main.fragment_connect_guardian.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.rfcx.companion.R
import org.rfcx.companion.connection.socket.AdminSocketManager
import org.rfcx.companion.connection.socket.GuardianSocketManager
import org.rfcx.companion.connection.wifi.OnWifiListener
import org.rfcx.companion.connection.wifi.WifiHotspotManager
import org.rfcx.companion.entity.Screen
import org.rfcx.companion.util.Analytics
import org.rfcx.companion.util.WifiHotspotUtils
import org.rfcx.companion.view.deployment.guardian.GuardianDeploymentProtocol

class ConnectGuardianFragment : Fragment(), OnWifiListener, (ScanResult) -> Unit {
    private val guardianHotspotAdapter by lazy { GuardianHotspotAdapter(this) }
    private var deploymentProtocol: GuardianDeploymentProtocol? = null
    private lateinit var wifiHotspotManager: WifiHotspotManager

    private var guardianHotspot: ScanResult? = null

    private lateinit var countDownTimer: CountDownTimer

    private var connectionCount = 0

    private val analytics by lazy { context?.let { Analytics(it) } }

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

        deploymentProtocol?.hideToolbar()

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
            analytics?.trackConnectGuardianHotspotEvent()
            guardianHotspot?.let {
                if (WifiHotspotUtils.isConnectedWithGuardian(requireContext(), it.SSID)) {
                    checkConnection()
                } else {
                    wifiHotspotManager.connectTo(it, this)
                }
            }
        }

        retryGuardianButton.setOnClickListener {
            analytics?.trackRetryGuardianHotspotEvent()
            showLoading()
            hideRetry()
            hideNotFound()
            retryCountdown(SCAN)
            wifiHotspotManager.nearbyHotspot(this)
        }

        connectInstructionText.setOnClickListener {
            deploymentProtocol?.showConnectInstruction()
        }
    }

    override fun invoke(hotspot: ScanResult) {
        analytics?.trackSelectGuardianHotspotEvent()
        guardianHotspot = hotspot
        enableConnectButton()
    }

    override fun onScanReceive(result: List<ScanResult>) {
        hideLoading()
        hideNotFound()
        hideRetry()
        countDownTimer.cancel()
        guardianHotspotAdapter.items = result
    }

    override fun onWifiConnected() {
        // connect to SocketServer
        checkConnection()
    }

    private fun checkConnection() {
        GuardianSocketManager.getConnection()
        AdminSocketManager.connect()
        lifecycleScope.launch {
            withContext(Dispatchers.Main) {
                GuardianSocketManager.pingBlob.observe(viewLifecycleOwner, Observer {
                    requireActivity().runOnUiThread {
                        deploymentProtocol?.startGuardianRegister()
                    }
                })
            }
        }
    }

    private fun showLoading() {
        connectGuardianLoading?.visibility = View.VISIBLE
        connectGuardianButton.isEnabled = false
        connectGuardianButton.visibility = View.VISIBLE
        guardianHotspotRecyclerView.visibility = View.INVISIBLE
    }

    private fun hideLoading() {
        connectGuardianLoading.visibility = View.INVISIBLE
        guardianHotspotRecyclerView.visibility = View.VISIBLE
    }

    private fun showRetry() {
        retryGuardianButton.visibility = View.VISIBLE
        connectGuardianLoading.visibility = View.GONE
        connectGuardianButton.visibility = View.INVISIBLE
    }

    private fun hideRetry() {
        retryGuardianButton.visibility = View.INVISIBLE
    }

    private fun showNotFound() {
        notFoundTextView.visibility = View.VISIBLE
    }

    private fun hideNotFound() {
        notFoundTextView.visibility = View.GONE
    }

    private fun retryCountdown(state: String) {
        countDownTimer = object : CountDownTimer(20000, 1000) {
            override fun onFinish() {
                if (state == SCAN) {
                    showNotFound()
                    showRetry()
                    wifiHotspotManager.unRegisterReceiver()
                } else {
                    hideLoading()
                    enableConnectButton()
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

    override fun onResume() {
        super.onResume()
        analytics?.trackScreen(Screen.CONNECT_GUARDIAN)
    }

    companion object {
        private val SCAN = "scan"
        private val CONNECT = "connect"

        fun newInstance(): ConnectGuardianFragment {
            return ConnectGuardianFragment()
        }
    }
}
