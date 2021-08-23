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

    private fun populateAdapterWithInfo(expandableSoftwareUpdateStateList: MutableList<ExpandableSoftwareUpdateModel>) {
        softwareUpdateAdapter = SoftwareUpdateAdapter(
            this,
            expandableSoftwareUpdateStateList
        )
        softwareUpdateAdapter?.let {
            val layoutManager = LinearLayoutManager(context)
            guardianApkRecyclerView.layoutManager = layoutManager
            guardianApkRecyclerView.adapter = it
            guardianApkRecyclerView.addItemDecoration(
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

        val guardianMockData = listOf(
            StateSoftwareUpdate.Software.ApkVersion("Guardian v0.8.2-fix"),
            StateSoftwareUpdate.Software.ApkVersion("Guardian v0.8.2"),
            StateSoftwareUpdate.Software.ApkVersion("Guardian v0.8.1")
        )
        val app2 = listOf(
            StateSoftwareUpdate.Software.ApkVersion("Admin v0.6.83"),
            StateSoftwareUpdate.Software.ApkVersion("Admin v0.6.16"),
            StateSoftwareUpdate.Software.ApkVersion("Admin v0.6.8")
        )
        val app3 = listOf(
            StateSoftwareUpdate.Software.ApkVersion("Classify v0.8.2"),
            StateSoftwareUpdate.Software.ApkVersion("Classify v0.6.2"),
            StateSoftwareUpdate.Software.ApkVersion("Classify v0.6.0")
        )
        val app4 = listOf(
            StateSoftwareUpdate.Software.ApkVersion("Updater v0.6.8"),
            StateSoftwareUpdate.Software.ApkVersion("Updater v0.6.2"),
            StateSoftwareUpdate.Software.ApkVersion("Updater v0.6.0")
        )

        val mockData = mutableListOf<ExpandableSoftwareUpdateModel>()

        mockData.add(
            ExpandableSoftwareUpdateModel(
                1,
                StateSoftwareUpdate.Software("Guardian App", guardianMockData)
            )
        )
        mockData.add(
            ExpandableSoftwareUpdateModel(
                1,
                StateSoftwareUpdate.Software("Admin App", app2)
            )
        )
        mockData.add(
            ExpandableSoftwareUpdateModel(
                1,
                StateSoftwareUpdate.Software("Classify App", app3)
            )
        )
        mockData.add(
            ExpandableSoftwareUpdateModel(
                1,
                StateSoftwareUpdate.Software("Updater App", app4)
            )
        )
        populateAdapterWithInfo(mockData)
    }

    companion object {
        @JvmStatic
        fun newInstance() = SoftwareUpdateFragment()
    }

    override fun onItemClick(apkVersion: String) {
        Toast.makeText(context,"Clicked on $apkVersion",Toast.LENGTH_LONG).show()
    }
}
