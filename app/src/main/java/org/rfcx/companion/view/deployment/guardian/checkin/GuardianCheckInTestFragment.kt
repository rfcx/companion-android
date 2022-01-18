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
import org.rfcx.companion.connection.socket.GuardianSocketManager
import org.rfcx.companion.entity.Screen
import org.rfcx.companion.entity.socket.response.CheckIn
import org.rfcx.companion.entity.socket.response.CheckInTestResponse
import org.rfcx.companion.util.Analytics
import org.rfcx.companion.util.TimeAgo
import org.rfcx.companion.util.timestampToDateString
import org.rfcx.companion.view.deployment.guardian.GuardianDeploymentProtocol

class GuardianCheckInTestFragment : Fragment() {

    private var deploymentProtocol: GuardianDeploymentProtocol? = null

    private val analytics by lazy { context?.let { Analytics(it) } }

    private var state = ""
    private var apiUrl = ""

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
            it.setToolbarSubtitle()
            it.setMenuToolbar(false)
            it.showToolbar()
            it.setToolbarTitle()
        }

        setCheckInTestView()

        checkInFinishButton.setOnClickListener {
            analytics?.trackClickNextEvent(Screen.GUARDIAN_CHECKIN_TEST.id)
            deploymentProtocol?.nextStep()
        }
    }

    private fun setCheckInTestView() {
        GuardianSocketManager.pingBlob.observe(viewLifecycleOwner, Observer {
            val latestCheckin = deploymentProtocol?.getLatestCheckIn()
            latestCheckin?.let {
                if (it.has("mqtt")) {
                    val mqtt = it.get("mqtt").asJsonObject
                    checkInProtocolValueTextView.text = "mqtt"
                    checkInTimeValueTextView.text = timestampToDateString((mqtt.get("created_at").asString).toLongOrNull())
                    checkInFinishButton.isEnabled = true
                }
                if (it.has("sbd")) {
                    val sbd = it.get("sbd").asJsonObject
                    checkInProtocolValueTextView.text = "sbd"
                    checkInTimeValueTextView.text = timestampToDateString((sbd.get("created_at").asString).toLongOrNull())
                    checkInFinishButton.isEnabled = true
                }
                if (it.has("swm")) {
                    val swm = it.get("swm").asJsonObject
                    val unsent = deploymentProtocol?.getSwmUnsentMessages() ?: -1
                    checkInProtocolValueTextView.text = "swm"
                    checkInTimeValueTextView.text = timestampToDateString((swm.get("created_at").asString).toLongOrNull())
                    checkInQueueTextView.visibility = View.VISIBLE
                    checkInQueueValueTextView.visibility = View.VISIBLE
                    checkInQueueValueTextView.text = if(unsent != -1) "$unsent messages" else "unable to retrieve unsent message"
                    checkInFinishButton.isEnabled = true
                }
            }
        })
    }

    private fun getLastCheckInRelativeTime(): String {
        val lastCheckInTime = deploymentProtocol?.getLastCheckInTime()
        if (lastCheckInTime != null) {
            val timeDiff = System.currentTimeMillis() - lastCheckInTime
            return TimeAgo.toDuration(timeDiff) ?: getString(R.string.dash)
        }
        return getString(R.string.dash)
    }

    override fun onDetach() {
        super.onDetach()
        if (state == CHECKIN_SUCCESS) {
            GuardianSocketManager.checkInTest.value = CheckInTestResponse(CheckIn(apiUrl = apiUrl, state = "not published"))
        }
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
