package org.rfcx.audiomoth.view.deployment.guardian.connect

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import kotlinx.android.synthetic.main.fragment_connect_guardian.*
import org.rfcx.audiomoth.R
import org.rfcx.audiomoth.view.deployment.DeploymentProtocol

class ConnectGuardianFragment : Fragment() {
    private var deploymentProtocol: DeploymentProtocol? = null

    override fun onAttach(context: Context) {
        super.onAttach(context)
        deploymentProtocol = (context as DeploymentProtocol)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_connect_guardian, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        deploymentProtocol?.hideCompleteButton()

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

    private fun showLoading() {
        connectGuardianLoading.visibility = View.VISIBLE
        nearbyGuardianTextView.visibility = View.GONE
        connectGuardianButton.visibility = View.GONE
    }

    private fun hideLoading() {
        connectGuardianLoading.visibility = View.GONE
        nearbyGuardianTextView.visibility = View.VISIBLE
        connectGuardianButton.visibility = View.VISIBLE
    }

    companion object {
        val TAG = "ConnectGuardianFragment"
        enum class ConnectionState { SUCCESS, FAILED}

        fun newInstance(): ConnectGuardianFragment {
            return ConnectGuardianFragment()
        }
    }
}
