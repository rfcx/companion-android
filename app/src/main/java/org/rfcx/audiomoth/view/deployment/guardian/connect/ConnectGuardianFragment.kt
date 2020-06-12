package org.rfcx.audiomoth.view.deployment.guardian.connect

import android.content.Context
import android.net.wifi.ScanResult
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.android.synthetic.main.fragment_connect_guardian.*
import org.rfcx.audiomoth.R
import org.rfcx.audiomoth.connection.wifi.OnScanReceiver
import org.rfcx.audiomoth.connection.wifi.WifiHotspotManager
import org.rfcx.audiomoth.view.deployment.guardian.GuardianDeploymentProtocol

class ConnectGuardianFragment : Fragment(), (ScanResult) -> Unit {
    private val guardianHotspotAdapter by lazy { GuardianHotspotAdapter(this) }
    private var deploymentProtocol: GuardianDeploymentProtocol? = null
    private lateinit var wifiHotspotManager: WifiHotspotManager

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
        wifiHotspotManager.nearbyHotspot(object: OnScanReceiver{
            override fun onReceive(result: List<ScanResult>) {
                hideLoading()
                guardianHotspotAdapter.items = result
            }
        })

        connectGuardianButton.setOnClickListener {
            //TODO: Call Socket command to get response back
            showLoading()

            val connectionState = ConnectionState.SUCCESS // replace with socket calling
            if (connectionState == ConnectionState.SUCCESS) {
                deploymentProtocol!!.nextStep()
            } else {
                //TODO: Show warning that connection is failed
                hideLoading()
            }
        }
    }

    override fun invoke(p1: ScanResult) {
        TODO("Not yet implemented")
    }

    private fun showLoading() {
        connectGuardianLoading.visibility = View.VISIBLE
        connectGuardianButton.visibility = View.GONE
    }

    private fun hideLoading() {
        connectGuardianLoading.visibility = View.GONE
        connectGuardianButton.visibility = View.VISIBLE
    }

    override fun onDestroyView() {
        super.onDestroyView()
        wifiHotspotManager.unRegisterReceiver()
    }

    companion object {
        val TAG = "ConnectGuardianFragment"
        enum class ConnectionState { SUCCESS, FAILED}

        fun newInstance(): ConnectGuardianFragment {
            return ConnectGuardianFragment()
        }
    }
}
