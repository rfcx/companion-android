package org.rfcx.audiomoth.view.deployment.guardian.checkin

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import kotlinx.android.synthetic.main.fragment_guardian_checkin_test.*
import org.rfcx.audiomoth.R
import org.rfcx.audiomoth.connection.socket.SocketManager
import org.rfcx.audiomoth.view.deployment.guardian.GuardianDeploymentProtocol

class GuardianCheckInTestFragment : Fragment() {

    private var deploymentProtocol: GuardianDeploymentProtocol? = null

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

        setCheckInTestView()

        checkInFinishButton.setOnClickListener {
            deploymentProtocol?.nextStep()
        }
    }

    private fun setCheckInTestView() {
        SocketManager.getCheckInTest()
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

    companion object {
        private const val CHECKIN_SUCCESS = "published"

        fun newInstance(): GuardianCheckInTestFragment = GuardianCheckInTestFragment()
    }
}
