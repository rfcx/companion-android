package org.rfcx.companion.view.deployment.songmeter

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.android.synthetic.main.fragment_song_meter_check_list.*
import org.rfcx.companion.R
import org.rfcx.companion.adapter.CheckListItem
import org.rfcx.companion.view.deployment.CheckListAdapter

class SongMeterCheckListFragment : Fragment(), (Int, String) -> Unit {
    private var deploymentProtocol: SongMeterDeploymentProtocol? = null
    private val checkListRecyclerView by lazy { CheckListAdapter(this) }

    companion object {
        @JvmStatic
        fun newInstance() = SongMeterCheckListFragment()
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        deploymentProtocol = (context as SongMeterDeploymentProtocol)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_songmeter_check_list, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        deploymentProtocol?.hideToolbar()
        setupAdapter()
        setupButton()
    }

    private fun setupAdapter() {
        songMeterCheckListRecyclerView.apply {
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
            adapter = checkListRecyclerView
        }
        checkListRecyclerView.setCheckList(getAllChecks())

        // set passed checks
        deploymentProtocol?.getPassedChecks()?.forEach { number ->
            checkListRecyclerView.setCheckPassed(number)
        }
    }

    private fun setupButton() {
        songMeterChecklistDeployButton.isEnabled = checkListRecyclerView.isEveryCheckListPassed()
        songMeterChecklistDeployButton.setOnClickListener {
            deploymentProtocol?.setReadyToDeploy()
        }
    }

    private fun getAllChecks(): List<CheckListItem> {
        val checkList = arrayListOf<CheckListItem>()
        var number = 0

        checkList.add(CheckListItem.Header(getString(R.string.setup)))
        val setupChecks =
            requireContext().resources.getStringArray(R.array.song_meter_setup_checks).toList()
        setupChecks.forEach { name ->
            checkList.add(CheckListItem.CheckItem(number, name, isRequired = true))
            number++
        }

        checkList.add(CheckListItem.Header(getString(R.string.optional)))
        val optionalChecks =
            requireContext().resources.getStringArray(R.array.song_meter_optional_checks).toList()
        optionalChecks.forEach { name ->
            checkList.add(CheckListItem.CheckItem(number, name, isRequired = false))
            number++
        }

        return checkList
    }

    override fun invoke(number: Int, name: String) {
        deploymentProtocol?.handleCheckClicked(number)
        deploymentProtocol?.setCurrentPage(name)
    }
}
