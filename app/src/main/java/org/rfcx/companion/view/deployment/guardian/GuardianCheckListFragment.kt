package org.rfcx.companion.view.deployment.guardian

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.android.synthetic.main.fragment_guardian_checklist.*
import org.rfcx.companion.R
import org.rfcx.companion.adapter.CheckListItem
import org.rfcx.companion.util.SocketUtils
import org.rfcx.companion.view.deployment.CheckListAdapter

class GuardianCheckListFragment : Fragment(), (Int, String) -> Unit {

    private var deploymentProtocol: GuardianDeploymentProtocol? = null

    private val checkListRecyclerView by lazy { CheckListAdapter(this) }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        deploymentProtocol = (context as GuardianDeploymentProtocol)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_guardian_checklist, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        deploymentProtocol?.let {
            context?.getString(R.string.setting_up_checklist)?.let { it1 -> it.setCurrentPage(it1) }
            it.setToolbarSubtitle("${it.getGuid()}")
            it.setMenuToolbar(true)
            it.showToolbar()
            it.setToolbarTitle()
        }

        guardianCheckListRecyclerView.apply {
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
            adapter = checkListRecyclerView
        }

        checkListRecyclerView.setCheckList(getAllChecks())
        // set passed checks
        deploymentProtocol?.getPassedChecks()?.forEach { number ->
            checkListRecyclerView.setCheckPassed(number)
        }

        checklistDeployButton.isEnabled = checkListRecyclerView.isEveryCheckListPassed()
        checklistDeployButton.setOnClickListener {
            deploy()
        }
    }

    override fun invoke(number: Int, name: String) {
        deploymentProtocol?.handleCheckClicked(number)
        deploymentProtocol?.reTriggerConnection()
        deploymentProtocol?.setCurrentPage(name)
    }

    private fun getAllChecks(): List<CheckListItem> {
        val checkList = arrayListOf<CheckListItem>()
        var number = 0

        checkList.add(CheckListItem.Header("Assembly"))
        val assemblyChecks = requireContext().resources.getStringArray(R.array.guardian_assembly_checks).toList()
        assemblyChecks.forEach { name ->
            checkList.add(CheckListItem.CheckItem(number, name, isRequired = false))
            number++
        }

        checkList.add(CheckListItem.Header("Setup"))
        val setupChecks = requireContext().resources.getStringArray(R.array.guardian_setup_checks).toList()
        setupChecks.forEach { name ->
            checkList.add(CheckListItem.CheckItem(number, name, isRequired = true))
            number++
        }

        checkList.add(CheckListItem.Header("Optional"))
        val optionalChecks = requireContext().resources.getStringArray(R.array.guardian_optional_checks).toList()
        optionalChecks.forEach { name ->
            checkList.add(CheckListItem.CheckItem(number, name, isRequired = false))
            number++
        }

        return checkList
    }

    private fun deploy() {
        deploymentProtocol?.setReadyToDeploy()
        SocketUtils.stopAllConnections()
    }

    override fun onResume() {
        super.onResume()
        deploymentProtocol?.let {
            it.setToolbarSubtitle("${it.getGuid()}")
        }
    }

    companion object {
        fun newInstance(): GuardianCheckListFragment {
            return GuardianCheckListFragment()
        }
    }
}
