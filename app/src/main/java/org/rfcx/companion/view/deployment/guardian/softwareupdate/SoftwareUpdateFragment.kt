package org.rfcx.companion.view.deployment.guardian.softwareupdate

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.android.synthetic.main.fragment_software_update.*
import org.rfcx.companion.R
import org.rfcx.companion.connection.socket.FileSocketManager
import org.rfcx.companion.connection.socket.GuardianSocketManager
import org.rfcx.companion.entity.Software
import org.rfcx.companion.entity.socket.request.CheckinCommand
import org.rfcx.companion.util.file.APKUtils
import org.rfcx.companion.view.deployment.guardian.GuardianDeploymentProtocol

class SoftwareUpdateFragment : Fragment(), ChildrenClickedListener {
    private var deploymentProtocol: GuardianDeploymentProtocol? = null

    var softwareUpdateAdapter: SoftwareUpdateAdapter? = null
    private var selectedFiles = mutableMapOf<String, String>()

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
            it.showToolbar()
            it.setToolbarTitle()
        }

        updateButton.setOnClickListener {
            var start = 0
            val keys = selectedFiles.keys.toList()
            sendFile(keys[start])
            FileSocketManager.pingBlob.observe(viewLifecycleOwner, Observer {
                requireActivity().runOnUiThread {
                    if (it.has(keys[start]) && it.get(keys[start]).asBoolean) {
                        selectedFiles.remove(keys[start])
                        if (selectedFiles.isNotEmpty()) {
                            start++
                            sendFile(keys[start])
                        } else {
                        }
                    }
                }
            })
        }

        val softwares = APKUtils.getAllDownloadedSoftwaresWithType(requireContext())
        populateAdapterWithInfo(softwares)
    }

    private fun sendFile(name: String) {
        selectedFiles[name]?.also {
            FileSocketManager.sendFile(it)
        }
    }

    companion object {
        @JvmStatic
        fun newInstance() = SoftwareUpdateFragment()
    }

    override fun onItemClick(selectedSoftwares: Map<String, String>) {
        updateButton.isEnabled = selectedSoftwares.isNotEmpty()
        selectedFiles.clear()
        selectedFiles.putAll(selectedSoftwares)
    }
}
