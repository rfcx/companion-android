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
import org.rfcx.companion.view.deployment.guardian.GuardianDeploymentProtocol

class SoftwareUpdateFragment : Fragment(), OnVersionClickedListener {
    private var deploymentProtocol: GuardianDeploymentProtocol? = null
    var softwareUpdateAdapter: SoftwareUpdateAdapter? = null

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

    private fun populateAdapterWithInfo(expandableSoftwareUpdateStateList: MutableList<ExpandableSoftwareUpdateModel>) {
        softwareUpdateAdapter = SoftwareUpdateAdapter(
            this,
            expandableSoftwareUpdateStateList
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

        updateButton.setOnClickListener {
            deploymentProtocol?.nextStep()
        }

        val guardianMockData = listOf(
            StateSoftwareUpdate.Software.Version("Guardian v0.8.2-fix"),
            StateSoftwareUpdate.Software.Version("Guardian v0.8.2"),
            StateSoftwareUpdate.Software.Version("Guardian v0.8.1")
        )
        val app2 = listOf(
            StateSoftwareUpdate.Software.Version("Admin v2.6.8"),
            StateSoftwareUpdate.Software.Version("Admin v1.7.1"),
            StateSoftwareUpdate.Software.Version("Admin v0.6.8")
        )
        val app3 = listOf(
            StateSoftwareUpdate.Software.Version("Classify v2.8.2"),
            StateSoftwareUpdate.Software.Version("Classify v2.6.2"),
            StateSoftwareUpdate.Software.Version("Classify v1.6.0")
        )
        val app4 = listOf(
            StateSoftwareUpdate.Software.Version("Updater v3.6.8"),
            StateSoftwareUpdate.Software.Version("Updater v3.6.2"),
            StateSoftwareUpdate.Software.Version("Updater v3.6.0")
        )

        val mockData = mutableListOf<ExpandableSoftwareUpdateModel>()

        mockData.add(
            ExpandableSoftwareUpdateModel(
                1,
                StateSoftwareUpdate.Software("Guardian App (current: v0.8.3)", guardianMockData)
            )
        )
        mockData.add(
            ExpandableSoftwareUpdateModel(
                1,
                StateSoftwareUpdate.Software("Admin App (current: v1.7.0)", app2)
            )
        )
        mockData.add(
            ExpandableSoftwareUpdateModel(
                1,
                StateSoftwareUpdate.Software("Classify App (current: v2.1.2)", app3)
            )
        )
        mockData.add(
            ExpandableSoftwareUpdateModel(
                1,
                StateSoftwareUpdate.Software("Updater App (current: v3.5.0)", app4)
            )
        )
        populateAdapterWithInfo(mockData)
    }

    companion object {
        @JvmStatic
        fun newInstance() = SoftwareUpdateFragment()
    }

    override fun onItemClick(version: String) {
        Toast.makeText(context,"Clicked on $version",Toast.LENGTH_LONG).show()
    }
}
