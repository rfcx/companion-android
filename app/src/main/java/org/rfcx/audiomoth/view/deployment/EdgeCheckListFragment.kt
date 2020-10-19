package org.rfcx.audiomoth.view.deployment

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.android.synthetic.main.fragment_edge_checklist.*
import org.rfcx.audiomoth.R
import org.rfcx.audiomoth.adapter.CheckListItem

class EdgeCheckListFragment : Fragment(), (Int, String) -> Unit {

    private var deploymentProtocol: EdgeDeploymentProtocol? = null

    private val checkListRecyclerView by lazy { CheckListAdapter(this) }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        deploymentProtocol = (context as EdgeDeploymentProtocol)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_edge_checklist, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        deploymentProtocol?.hideToolbar()

        edgeCheckListRecyclerView.apply {
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
            adapter = checkListRecyclerView
        }

        checkListRecyclerView.setCheckList(getAllChecks())
        // set passed checks
        deploymentProtocol?.getPassedChecks()?.forEach { number ->
            checkListRecyclerView.setCheckPassed(number)
        }

        edgeChecklistDeployButton.isEnabled = checkListRecyclerView.isEveryCheckListPassed()
        edgeChecklistDeployButton.setOnClickListener {
            deploymentProtocol?.setReadyToDeploy()
        }
    }

    override fun invoke(number: Int, name: String) {
        deploymentProtocol?.handleCheckClicked(number)
        deploymentProtocol?.setCurrentPage(name)
    }

    private fun getAllChecks(): List<CheckListItem> {
        val checkList = arrayListOf<CheckListItem>()
        var number = 0

        checkList.add(CheckListItem.Header("Setup"))
        val setupChecks = requireContext().resources.getStringArray(R.array.edge_setup_checks).toList()
        setupChecks.forEach { name ->
            checkList.add(CheckListItem.CheckItem(number, name, isRequired = true))
            number++
        }

        checkList.add(CheckListItem.Header("Optional"))
        val optionalChecks = requireContext().resources.getStringArray(R.array.edge_optional_checks).toList()
        optionalChecks.forEach { name ->
            checkList.add(CheckListItem.CheckItem(number, name, isRequired = false))
            number++
        }

        return checkList
    }

    companion object {
        fun newInstance(): EdgeCheckListFragment {
            return EdgeCheckListFragment()
        }
    }
}
