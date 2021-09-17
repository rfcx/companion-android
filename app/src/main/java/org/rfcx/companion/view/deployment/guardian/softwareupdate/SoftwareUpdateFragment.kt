package org.rfcx.companion.view.deployment.guardian.softwareupdate

import android.content.Context
import android.os.Bundle
import android.os.CountDownTimer
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.android.synthetic.main.fragment_software_update.*
import org.rfcx.companion.R
import org.rfcx.companion.connection.socket.FileSocketManager
import org.rfcx.companion.connection.socket.GuardianSocketManager
import org.rfcx.companion.entity.Software
import org.rfcx.companion.util.file.APKUtils
import org.rfcx.companion.util.file.APKUtils.calculateVersionValue
import org.rfcx.companion.view.deployment.guardian.GuardianDeploymentProtocol
import java.util.*

class SoftwareUpdateFragment : Fragment(), ChildrenClickedListener {
    private var deploymentProtocol: GuardianDeploymentProtocol? = null
    var softwareUpdateAdapter: SoftwareUpdateAdapter? = null
    private var selectedFile: StateSoftwareUpdate.SoftwareChildren? = null
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

    private fun populateAdapterWithInfo(softwares: List<Software>) {
        softwareUpdateAdapter = SoftwareUpdateAdapter(
            this,
            softwares
        )
        softwareUpdateAdapter?.let {
            val layoutManager = LinearLayoutManager(context)
            apkRecyclerView.layoutManager = layoutManager
            apkRecyclerView.adapter = it
            apkRecyclerView.addItemDecoration(
                DividerItemDecoration(
                    this.context,
                    DividerItemDecoration.VERTICAL
                )
            )
            it.notifyDataSetChanged()
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        deploymentProtocol?.let {
            it.setToolbarSubtitle()
            it.setMenuToolbar(false)
            it.showToolbar()
            it.setToolbarTitle()
        }

        GuardianSocketManager.pingBlob.observe(viewLifecycleOwner, Observer {
            requireActivity().runOnUiThread {
                deploymentProtocol?.getSoftwareVersion()?.let {
                    softwareUpdateAdapter?.guardianSoftwareVersion = it
                    softwareUpdateAdapter?.notifyDataSetChanged()

                    selectedFile?.let { selected ->
                        val selectedVersion = selected.version
                        val installedVersion = it[selected.parent]
                        if (installedVersion != null && calculateVersionValue(installedVersion) == calculateVersionValue(selectedVersion)) {
                            softwareUpdateAdapter?.hideLoading()
                            nextButton.isEnabled = true

                            stopTimer()
                        }
                    }
                }
            }
        })

        nextButton.setOnClickListener {
            deploymentProtocol?.nextStep()
        }

        val softwares = APKUtils.getAllDownloadedSoftwaresWithType(requireContext())
        populateAdapterWithInfo(softwares)
    }

    private fun startTimer() {
        loadingTimer = object: CountDownTimer(60000, 1000) {
            override fun onTick(millisUntilFinished: Long) { }

            override fun onFinish() {
                softwareUpdateAdapter?.let {
                    if (it.getLoading()) {
                        it.hideLoading()
                    }
                }
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
        fun newInstance() = SoftwareUpdateFragment()
    }

    override fun onItemClick(selectedSoftware: StateSoftwareUpdate.SoftwareChildren) {
        selectedFile = selectedSoftware
        FileSocketManager.sendFile(selectedSoftware.path)
        nextButton.isEnabled = false
        startTimer()
    }
}
