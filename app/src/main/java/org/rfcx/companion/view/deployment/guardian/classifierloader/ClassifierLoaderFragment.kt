package org.rfcx.companion.view.deployment.guardian.classifierloader

import android.content.Context
import android.os.Bundle
import android.os.CountDownTimer
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import kotlinx.android.synthetic.main.fragment_software_update.*
import org.rfcx.companion.R
import org.rfcx.companion.connection.socket.FileSocketManager
import org.rfcx.companion.util.file.ClassifierUtils
import org.rfcx.companion.view.deployment.guardian.GuardianDeploymentProtocol
import java.util.*

class ClassifierLoaderFragment : Fragment() {
    private var deploymentProtocol: GuardianDeploymentProtocol? = null
    private var loadingTimer: CountDownTimer? = null

    override fun onAttach(context: Context) {
        super.onAttach(context)
        deploymentProtocol = (context as GuardianDeploymentProtocol)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_software_update, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        deploymentProtocol?.let {
            it.setToolbarSubtitle()
            it.setMenuToolbar(false)
            it.showToolbar()
            it.setToolbarTitle()
        }

        nextButton.setOnClickListener {
            deploymentProtocol?.nextStep()
        }

        val softwares = ClassifierUtils.getAllDownloadedClassifiers(requireContext())
        if (softwares.isNullOrEmpty()) {
            noSoftwareText.visibility = View.VISIBLE
        } else {
//            populateAdapterWithInfo(softwares)
        }
    }

    private fun startTimer() {
        loadingTimer = object: CountDownTimer(60000, 1000) {
            override fun onTick(millisUntilFinished: Long) { }

            override fun onFinish() {
//                softwareUpdateAdapter?.let {
//                    if (it.getLoading()) {
//                        it.hideLoading()
//                    }
//                }
                stopTimer()
            }
        }
        loadingTimer?.start()
    }

    private fun stopTimer() {
        loadingTimer?.cancel()
        loadingTimer = null
    }

    companion object {
        @JvmStatic
        fun newInstance() = ClassifierLoaderFragment()
    }

//    override fun onItemClick(selectedSoftware: SoftwareItem.SoftwareVersion) {
//        selectedFile = selectedSoftware
//        FileSocketManager.sendFile(selectedSoftware.path)
//        nextButton.isEnabled = false
//        startTimer()
//    }
}
