package org.rfcx.companion.view.deployment

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import org.rfcx.companion.R
import org.rfcx.companion.adapter.CheckListItem
import org.rfcx.companion.databinding.FragmentEdgeChecklistBinding

class AudioMothCheckListFragment : Fragment(), (Int, String) -> Unit {

    private var deploymentProtocol: AudioMothDeploymentProtocol? = null

    private val checkListRecyclerView by lazy { CheckListAdapter(this) }

    private var _binding: FragmentEdgeChecklistBinding? = null
    private val binding get() = _binding!!

    override fun onAttach(context: Context) {
        super.onAttach(context)
        deploymentProtocol = (context as AudioMothDeploymentProtocol)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentEdgeChecklistBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        deploymentProtocol?.let {
            it.showToolbar()
            it.setCurrentPage(getString(R.string.setting_up_edge_checklist))
            it.setToolbarTitle()
        }

        binding.edgeCheckListRecyclerView.apply {
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
            adapter = checkListRecyclerView
        }

        checkListRecyclerView.setCheckList(getAllChecks())
        // set passed checks
        deploymentProtocol?.getPassedChecks()?.forEach { number ->
            checkListRecyclerView.setCheckPassed(number)
        }

        binding.edgeChecklistDeployButton.isEnabled = checkListRecyclerView.isEveryCheckListPassed()
        binding.edgeChecklistDeployButton.setOnClickListener {
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

        checkList.add(CheckListItem.Header(getString(R.string.setup)))
        val setupChecks = requireContext().resources.getStringArray(R.array.edge_setup_checks).toList()
        setupChecks.forEach { name ->
            checkList.add(CheckListItem.CheckItem(number, name, isRequired = true))
            number++
        }

        checkList.add(CheckListItem.Header(getString(R.string.optional)))
        val optionalChecks = requireContext().resources.getStringArray(R.array.edge_optional_checks).toList()
        optionalChecks.forEach { name ->
            checkList.add(CheckListItem.CheckItem(number, name, isRequired = false))
            number++
        }

        return checkList
    }

    companion object {
        fun newInstance(): AudioMothCheckListFragment {
            return AudioMothCheckListFragment()
        }
    }
}
