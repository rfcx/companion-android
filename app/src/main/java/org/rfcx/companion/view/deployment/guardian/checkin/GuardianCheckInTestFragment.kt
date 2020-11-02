package org.rfcx.companion.view.deployment.guardian.checkin

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import kotlinx.android.synthetic.main.fragment_guardian_checkin_test.*
import org.rfcx.companion.R
import org.rfcx.companion.connection.socket.SocketManager
import org.rfcx.companion.entity.Screen
import org.rfcx.companion.util.Analytics
import org.rfcx.companion.view.deployment.guardian.GuardianDeploymentProtocol

class GuardianCheckInTestFragment : Fragment() {

    private var deploymentProtocol: GuardianDeploymentProtocol? = null
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
        return inflater.inflate(R.layout.fragment_guardian_checkin_test, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        deploymentProtocol?.let {
            it.showToolbar()
            it.setToolbarTitle()
        }

        setCheckInTestView()

        checkInFinishButton.setOnClickListener {
            deploymentProtocol?.nextStep()
        }
    }

    private fun setCheckInTestView() {
        SocketManager.checkInTest.observe(viewLifecycleOwner, Observer { res ->
            checkInUrlValueTextView.text = res.checkin.apiUrl
            checkInStatusValueTextView.text = res.checkin.state
            checkInDeliveryTimeValueTextView.text = res.checkin.deliveryTime

            checkInFinishButton.isEnabled = res.checkin.state == CHECKIN_SUCCESS
        })
    }

    override fun onDetach() {
        super.onDetach()
        SocketManager.getCheckInTest() // to stop listening checkin test
    }

    override fun onResume() {
        super.onResume()
        analytics?.trackScreen(Screen.GUARDIAN_CHECKIN_TEST)
    }

    companion object {
        private const val CHECKIN_SUCCESS = "published"

        fun newInstance(): GuardianCheckInTestFragment = GuardianCheckInTestFragment()
    }
}