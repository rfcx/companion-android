package org.rfcx.companion.view.deployment.guardian.softwareupdate

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.android.synthetic.main.fragment_software_update.*
import org.rfcx.companion.R
import org.rfcx.companion.entity.Software
import org.rfcx.companion.util.file.APKUtils
import org.rfcx.companion.view.deployment.guardian.GuardianDeploymentProtocol

class SoftwareUpdateFragment : Fragment(), CountryClickedListener {
    private var deploymentProtocol: GuardianDeploymentProtocol? = null

    var softwareUpdateAdapter: SoftwareUpdateAdapter? = null
    private var selectedApk = ""

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
            deploymentProtocol?.nextStep()
        }

        val softwares = APKUtils.getAllDownloadedSoftwaresWithType(requireContext())
        populateAdapterWithInfo(softwares)
    }

    companion object {
        @JvmStatic
        fun newInstance() = SoftwareUpdateFragment()
    }

    override fun onItemClick(apkVersion: String) {
        Toast.makeText(context,"Clicked on $apkVersion",Toast.LENGTH_LONG).show()
    }
}
