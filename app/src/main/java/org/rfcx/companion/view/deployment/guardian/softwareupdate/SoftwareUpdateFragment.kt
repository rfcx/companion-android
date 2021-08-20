package org.rfcx.companion.view.deployment.guardian.softwareupdate

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.android.synthetic.main.fragment_software_update.*
import org.rfcx.companion.R
import org.rfcx.companion.view.deployment.guardian.GuardianDeploymentProtocol

class SoftwareUpdateFragment : Fragment(), (String) -> Unit {
    private var deploymentProtocol: GuardianDeploymentProtocol? = null

    private val guardianApkAdapter by lazy { GuardianApkAdapter(this) }
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

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        deploymentProtocol?.let {
            it.showToolbar()
            it.setToolbarTitle()
        }

        guardianApkRecyclerView.apply {
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
            adapter = guardianApkAdapter
        }

        guardianApkAdapter.items = listOf(
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
    }

    companion object {
        @JvmStatic
        fun newInstance() = SoftwareUpdateFragment()
    }

    override fun invoke(versionApk: String) {
        selectedApk = versionApk
        updateButton.isEnabled = true
    }
}
