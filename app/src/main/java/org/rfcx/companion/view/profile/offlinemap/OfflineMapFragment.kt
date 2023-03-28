package org.rfcx.companion.view.profile.offlinemap

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.mapbox.mapboxsdk.offline.*
import kotlinx.android.synthetic.main.fragment_offline_map.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.rfcx.companion.R
import org.rfcx.companion.base.ViewModelFactory
import org.rfcx.companion.entity.OfflineMapState
import org.rfcx.companion.entity.Project
import org.rfcx.companion.repo.api.CoreApiHelper
import org.rfcx.companion.repo.api.CoreApiServiceImpl
import org.rfcx.companion.repo.api.DeviceApiHelper
import org.rfcx.companion.repo.api.DeviceApiServiceImpl
import org.rfcx.companion.repo.local.LocalDataHelper
import org.rfcx.companion.util.Preferences
import org.rfcx.companion.util.isNetworkAvailable

class OfflineMapFragment : Fragment(), ProjectOfflineMapListener {

    companion object {
        const val TAG = "OfflineMapFragment"

        @JvmStatic
        fun newInstance() = OfflineMapFragment()
    }

    private lateinit var projectOfflineMapViewModel: ProjectOfflineMapViewModel

    lateinit var projectAdapter: ProjectOfflineMapAdapter
    private var project: Project? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_offline_map, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val job = Job()
        val scope = CoroutineScope(Dispatchers.IO + job)
        scope.launch {
            job.cancel()
        }

        setViewModel()
        setObserver()
        setupAdapter()
    }

    private fun setViewModel() {
        projectOfflineMapViewModel = ViewModelProvider(
            this,
            ViewModelFactory(
                requireActivity().application,
                DeviceApiHelper(DeviceApiServiceImpl(requireContext())),
                CoreApiHelper(CoreApiServiceImpl(requireContext())),
                LocalDataHelper()
            )
        ).get(ProjectOfflineMapViewModel::class.java)
    }

    private fun setObserver() {
        projectOfflineMapViewModel.getProjects().observe(
            viewLifecycleOwner,
            Observer {
                if (projectOfflineMapViewModel.getOfflineDownloading() == null) {
                    projectAdapter.hideDownloadButton =
                        projectOfflineMapViewModel.getOfflineDownloading() != null
                }
            }
        )

        projectOfflineMapViewModel.getStateOfflineMap().observe(
            viewLifecycleOwner,
            Observer {
                setStateOfflineMap(it)
            }
        )

        projectOfflineMapViewModel.getPercentageDownloads()
            .observe(
                viewLifecycleOwner,
                Observer { percentage ->
                    if (percentage >= 100) {
                        projectOfflineMapViewModel.updateOfflineDownloadedState()
                        setStateOfflineMap(OfflineMapState.DOWNLOADED_STATE.key)
                        this.project?.let { it1 -> projectAdapter.setDownloading(it1) }
                    } else {
                        this.project?.let { it1 -> projectAdapter.setProgress(it1, percentage) }
                        setStateOfflineMap(OfflineMapState.DOWNLOADING_STATE.key)
                    }
                }
            )

        projectOfflineMapViewModel.hideDownloadButton().observe(
            viewLifecycleOwner,
            Observer {
                projectAdapter.hideDownloadButton = it
            }
        )
    }

    private fun setupAdapter() {
        with(projectsRecyclerView) {
            layoutManager = LinearLayoutManager(context)
            DividerItemDecoration(
                context,
                (layoutManager as LinearLayoutManager).orientation
            ).apply {
                addItemDecoration(this)
            }
            projectAdapter = ProjectOfflineMapAdapter(
                projectOfflineMapViewModel.getProjectsFromLocal(),
                this@OfflineMapFragment
            )
            adapter = projectAdapter
        }
        projectAdapter.hideDownloadButton =
            projectOfflineMapViewModel.getOfflineDownloading() != null

        if (projectOfflineMapViewModel.getOfflineDownloading() != null) {
            if (context.isNetworkAvailable()) {
                this.project = projectOfflineMapViewModel.getOfflineDownloading()!!
                projectOfflineMapViewModel.offlineMapBox(projectOfflineMapViewModel.getOfflineDownloading()!!)
            } else {
                Toast.makeText(
                    context,
                    context?.getString(R.string.no_internet_connection),
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun setStateOfflineMap(state: String) {
        val preferences = context?.let { Preferences.getInstance(it) }
        preferences?.putString(Preferences.OFFLINE_MAP_STATE, state)
    }

    override fun onDownloadClicked(project: Project) {
        if (context.isNetworkAvailable()) {
            val preferences = context?.let { Preferences.getInstance(it) }
            preferences?.putString(Preferences.OFFLINE_MAP_SERVER_ID, project.serverId ?: "")
            projectOfflineMapViewModel.updateOfflineState(
                OfflineMapState.DOWNLOADING_STATE.key,
                project.serverId ?: ""
            )
            this.project = project
            projectOfflineMapViewModel.offlineMapBox(project)
        } else {
            Toast.makeText(
                context,
                context?.getString(R.string.no_internet_connection),
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    override fun onDeleteClicked(project: Project) {
        if (context.isNetworkAvailable()) {
            val preferences = context?.let { Preferences.getInstance(it) }
            preferences?.putString(Preferences.OFFLINE_MAP_SERVER_ID, project.serverId ?: "")
            projectOfflineMapViewModel.updateOfflineState(
                OfflineMapState.DELETING_STATE.key,
                project.serverId ?: ""
            )
            projectOfflineMapViewModel.deleteOfflineRegion(project)
        } else {
            Toast.makeText(
                context,
                context?.getString(R.string.no_internet_connection),
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        projectOfflineMapViewModel.onDestroy()
    }
}
