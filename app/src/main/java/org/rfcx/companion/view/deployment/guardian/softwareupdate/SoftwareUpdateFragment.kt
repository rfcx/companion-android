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

    private fun populateAdapterWithInfo(expandableCountryStateList: MutableList<ExpandableCountryModel>) {
        softwareUpdateAdapter = SoftwareUpdateAdapter(
            this,
            expandableCountryStateList
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

//        guardianApkRecyclerView.apply {
//            layoutManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
//            adapter = softwareUpdateAdapter
//        }

        updateButton.setOnClickListener {
            deploymentProtocol?.nextStep()
        }


        val app = listOf(
            "Guardian v0.8.2-fix",
            "Guardian v0.8.2",
            "Guardian v0.8.1",
            "Guardian v0.8.0",
            "Guardian v0.6.83",
            "Guardian v0.6.16",
            "Guardian v0.6.8",
            "Guardian v0.6.5",
            "Guardian v0.6.2",
            "Guardian v0.6.1",
            "Guardian v0.6.0",
            "Guardian v0.5.31"
        )

        val app1 = listOf(
            StateCapital.Country.State("Guardian v0.8.2-fix", "v0.8.2-fix"),
            StateCapital.Country.State("Guardian v0.8.2", "v0.8.2"),
            StateCapital.Country.State("Guardian v0.8.1", "v0.8.1")
        )
        val app2 = listOf(
            StateCapital.Country.State("admin  v0.8.2-fix", "v0.8.2-fix"),
            StateCapital.Country.State("admin  v0.8.2", "v0.8.2"),
            StateCapital.Country.State("admin  v0.8.1", "v0.8.1")
        )
        val app3 = listOf(
            StateCapital.Country.State("classify  v0.8.2-fix", "v0.8.2-fix"),
            StateCapital.Country.State("classify  v0.8.2", "v0.8.2"),
            StateCapital.Country.State("classify  v0.8.1", "v0.8.1")
        )
        val app4 = listOf(
            StateCapital.Country.State("updater v0.8.2-fix", "v0.8.2-fix"),
            StateCapital.Country.State("updater v0.8.2", "v0.8.2"),
            StateCapital.Country.State("updater v0.8.1", "v0.8.1")
        )

        val datas = mutableListOf<ExpandableCountryModel>()

        datas.add(
            ExpandableCountryModel(
                1,
                StateCapital.Country("Guardian", app1)
            )
        )
        datas.add(
            ExpandableCountryModel(
                1,
                StateCapital.Country("admin", app2)
            )
        )
        datas.add(
            ExpandableCountryModel(
                1,
                StateCapital.Country("classify", app3)
            )
        )
        datas.add(
            ExpandableCountryModel(
                1,
                StateCapital.Country("updater", app4)
            )
        )
        populateAdapterWithInfo(datas)
    }

    companion object {
        @JvmStatic
        fun newInstance() = SoftwareUpdateFragment()
    }

    override fun onItemClick(countryName: String, countryChild: StateCapital.Country.State) {
        Toast.makeText(context,"Clicked on $countryName with info ${countryChild.name} and ${countryChild.capital}",Toast.LENGTH_LONG).show()
    }
}
