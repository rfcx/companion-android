package org.rfcx.companion.view.deployment.guardian.classifier

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.android.synthetic.main.fragment_classifier.*
import org.rfcx.companion.R
import org.rfcx.companion.view.deployment.guardian.GuardianDeploymentProtocol
import org.rfcx.companion.view.deployment.guardian.softwareupdate.CountryClickedListener
import org.rfcx.companion.view.deployment.guardian.softwareupdate.ExpandableSoftwareUpdateModel
import org.rfcx.companion.view.deployment.guardian.softwareupdate.SoftwareUpdateAdapter
import org.rfcx.companion.view.deployment.guardian.softwareupdate.StateSoftwareUpdate

class ClassifierFragment : Fragment(), CountryClickedListener {
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
        return inflater.inflate(R.layout.fragment_classifier, container, false)
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

        mockData() // TODO:: Delete
    }

    private fun mockData() {
        val d1 = listOf(
            StateSoftwareUpdate.Software.ApkVersion("chainsaw v5"),
            StateSoftwareUpdate.Software.ApkVersion("chainsaw v4"),
            StateSoftwareUpdate.Software.ApkVersion("chainsaw v3"),
            StateSoftwareUpdate.Software.ApkVersion("chainsaw v2"),
            StateSoftwareUpdate.Software.ApkVersion("chainsaw v1"),
            StateSoftwareUpdate.Software.ApkVersion("chainsaw rfcx v1"),
            StateSoftwareUpdate.Software.ApkVersion("chainsaw hua v5"),
            StateSoftwareUpdate.Software.ApkVersion("chainsaw hua v4")
        )
        val d2 = listOf(
            StateSoftwareUpdate.Software.ApkVersion("vehicle v5"),
            StateSoftwareUpdate.Software.ApkVersion("vehicle v4"),
            StateSoftwareUpdate.Software.ApkVersion("vehicle v3"),
            StateSoftwareUpdate.Software.ApkVersion("vehicle v2"),
            StateSoftwareUpdate.Software.ApkVersion("vehicle v1"),
            StateSoftwareUpdate.Software.ApkVersion("vehicle rfcx v1")
        )
        val d3 = listOf(
            StateSoftwareUpdate.Software.ApkVersion("gunshot v14"),
            StateSoftwareUpdate.Software.ApkVersion("gunshot v13"),
            StateSoftwareUpdate.Software.ApkVersion("gunshot v12"),
            StateSoftwareUpdate.Software.ApkVersion("gunshot v7"),
            StateSoftwareUpdate.Software.ApkVersion("gunshot v4"),
            StateSoftwareUpdate.Software.ApkVersion("gunshot v3"),
            StateSoftwareUpdate.Software.ApkVersion("gunshot v1")
        )
        val d4 = listOf(
            StateSoftwareUpdate.Software.ApkVersion("whale orca v1"),
            StateSoftwareUpdate.Software.ApkVersion("whale v1")
        )

        val d5 = listOf(
            StateSoftwareUpdate.Software.ApkVersion("dog bark v12 "),
            StateSoftwareUpdate.Software.ApkVersion("dog bark v10 "),
            StateSoftwareUpdate.Software.ApkVersion("dog bark v9 "),
            StateSoftwareUpdate.Software.ApkVersion("dog bark v8 "),
            StateSoftwareUpdate.Software.ApkVersion("dog bark v7 "),
            StateSoftwareUpdate.Software.ApkVersion("dog bark v6 "),
            StateSoftwareUpdate.Software.ApkVersion("dog bark v4 "),
        )
        val d6 = listOf(
            StateSoftwareUpdate.Software.ApkVersion("voice v5"),
            StateSoftwareUpdate.Software.ApkVersion("voice v4"),
            StateSoftwareUpdate.Software.ApkVersion("voice v3"),
            StateSoftwareUpdate.Software.ApkVersion("voice v2"),
            StateSoftwareUpdate.Software.ApkVersion("voice v1")
        )

        val mockData = mutableListOf<ExpandableSoftwareUpdateModel>()

        mockData.add(
            ExpandableSoftwareUpdateModel(
                1,
                StateSoftwareUpdate.Software("Chainsaw", d1)
            )
        )
        mockData.add(
            ExpandableSoftwareUpdateModel(
                1,
                StateSoftwareUpdate.Software("Vehicle", d2)
            )
        )
        mockData.add(
            ExpandableSoftwareUpdateModel(
                1,
                StateSoftwareUpdate.Software("Gunshot", d3)
            )
        )
        mockData.add(
            ExpandableSoftwareUpdateModel(
                1,
                StateSoftwareUpdate.Software("Whale", d4)
            )
        )
        mockData.add(
            ExpandableSoftwareUpdateModel(
                1,
                StateSoftwareUpdate.Software("Dog bark", d5)
            )
        )
        mockData.add(
            ExpandableSoftwareUpdateModel(
                1,
                StateSoftwareUpdate.Software("Voice", d6)
            )
        )
        populateAdapterWithInfo(mockData)
    }

    private fun populateAdapterWithInfo(expandableSoftwareUpdateStateList: MutableList<ExpandableSoftwareUpdateModel>) {
        softwareUpdateAdapter = SoftwareUpdateAdapter(
            this,
            expandableSoftwareUpdateStateList
        )
        softwareUpdateAdapter?.let {
            val layoutManager = LinearLayoutManager(context)
            versionRecyclerView.layoutManager = layoutManager
            versionRecyclerView.adapter = it
            versionRecyclerView.addItemDecoration(
                DividerItemDecoration(
                    this.context,
                    DividerItemDecoration.VERTICAL
                )
            )
            it.notifyDataSetChanged()
        }
    }

    companion object {
        @JvmStatic
        fun newInstance() = ClassifierFragment()
    }

    override fun onItemClick(apkVersion: String) {
        Toast.makeText(context,"Clicked on $apkVersion", Toast.LENGTH_LONG).show()
    }
}
