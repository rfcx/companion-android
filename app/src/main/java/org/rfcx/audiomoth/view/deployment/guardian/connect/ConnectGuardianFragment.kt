package org.rfcx.audiomoth.view.deployment.guardian.connect

import android.app.Activity
import android.content.Context
import android.net.wifi.ScanResult
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.android.synthetic.main.fragment_connect_guardian.*
import org.rfcx.audiomoth.R
import org.rfcx.audiomoth.connection.socket.OnReceiveResponse
import org.rfcx.audiomoth.connection.socket.SocketManager
import org.rfcx.audiomoth.connection.wifi.OnWifiListener
import org.rfcx.audiomoth.connection.wifi.WifiHotspotManager
import org.rfcx.audiomoth.entity.socket.ConnectionResponse
import org.rfcx.audiomoth.entity.socket.SocketResposne
import org.rfcx.audiomoth.view.deployment.guardian.GuardianDeploymentProtocol

class ConnectGuardianFragment : Fragment(), OnWifiListener, (ScanResult) -> Unit {
    private val guardianHotspotAdapter by lazy { GuardianHotspotAdapter(this) }
    private var deploymentProtocol: GuardianDeploymentProtocol? = null
    private lateinit var wifiHotspotManager: WifiHotspotManager

    private var guardianHotspot: ScanResult? = null

    override fun onAttach(context: Context) {
        super.onAttach(context)
        deploymentProtocol = (context as GuardianDeploymentProtocol)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_connect_guardian, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        deploymentProtocol?.hideCompleteButton()
        showLoading()

        guardianHotspotRecyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = guardianHotspotAdapter
        }

        wifiHotspotManager = WifiHotspotManager(context!!.applicationContext)
        wifiHotspotManager.nearbyHotspot(this)

        connectGuardianButton.setOnClickListener {
            showLoading()
            wifiHotspotManager.connectTo(guardianHotspot!!, this)
        }
    }

    override fun invoke(hotspot: ScanResult) {
        guardianHotspot = hotspot
        enableConnectButton()
    }

    override fun onScanReceive(result: List<ScanResult>) {
        hideLoading()
        guardianHotspotAdapter.items = result
    }

    override fun onWifiConnected() {
        SocketManager.connect(object: OnReceiveResponse{
            override fun onReceive(response: SocketResposne) {
                activity!!.runOnUiThread {
                    val connection = response as ConnectionResponse
                    Log.d("ConenctionGuardian", connection.connection.status)
                    if (connection.connection.status == CONNECTION_SUCCESS) {
                        deploymentProtocol!!.nextStep()
                    } else {
                        hideLoading()
                    }
                }
            }

            override fun onFailed() {
                activity!!.runOnUiThread {
                    hideLoading()
                }
            }
        })
    }

    private fun showLoading() {
        connectGuardianLoading.visibility = View.VISIBLE
        connectGuardianButton.visibility = View.GONE
    }

    private fun hideLoading() {
        connectGuardianLoading.visibility = View.GONE
        connectGuardianButton.visibility = View.VISIBLE
    }

    private fun enableConnectButton() {
        connectGuardianButton.isEnabled = true
    }

    override fun onDestroy() {
        super.onDestroy()
        wifiHotspotManager.unRegisterReceiver()
    }

    companion object {
        private val CONNECTION_SUCCESS = "success"

        fun newInstance(): ConnectGuardianFragment {
            return ConnectGuardianFragment()
        }
    }
}
