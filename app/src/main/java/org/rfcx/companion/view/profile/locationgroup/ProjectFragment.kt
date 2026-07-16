package org.rfcx.companion.view.profile.locationgroup

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.map
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import io.realm.Realm
import org.rfcx.companion.R
import org.rfcx.companion.databinding.FragmentLocationGroupBinding
import org.rfcx.companion.entity.Project
import org.rfcx.companion.entity.response.ProjectResponse
import org.rfcx.companion.localdb.ProjectDb
import org.rfcx.companion.repo.ApiManager
import org.rfcx.companion.util.*
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class ProjectFragment :
    Fragment(),
    ProjectListener,
    SwipeRefreshLayout.OnRefreshListener {
    val realm: Realm = Realm.getInstance(RealmHelper.migrationConfig())
    private val projectGroupDb = ProjectDb(realm)

    private val projectAdapter by lazy { ProjectAdapter(this) }
    private var projectProtocol: ProjectProtocol? = null
    private var selectedProject: String? = null
    private var screen: String? = null

    private lateinit var locationGroupLiveData: LiveData<List<Project>>

    private var _binding: FragmentLocationGroupBinding? = null
    private val binding get() = _binding!!

    private val locationGroupObserve = Observer<List<Project>> {
        projectAdapter.apply {
            items = projectGroupDb.getProjects()
            notifyDataSetChanged()
        }
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        projectProtocol = (context as ProjectProtocol)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initIntent()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        _binding = FragmentLocationGroupBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.locationGroupRecyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = projectAdapter
        }

        binding.projectSwipeRefreshView.apply {
            setOnRefreshListener(this@ProjectFragment)
            setColorSchemeResources(R.color.colorPrimary)
        }

        projectAdapter.apply {
            this.selectedGroup = selectedGroup
            this.screen = screen
        }

        getProjects()
    }

    private fun initIntent() {
        arguments?.let {
            selectedProject = it.getString(ARG_PROJECT)
            screen = it.getString(ProjectActivity.EXTRA_SCREEN)
        }
    }

    private fun getProjects() {
        locationGroupLiveData =
            projectGroupDb.getAllResultsAsync().asLiveData().map {
                it
            }
        locationGroupLiveData.observeForever(locationGroupObserve)
    }

    private fun retrieveProjects(context: Context) {
        ApiManager.getInstance().getDeviceApi2(context).getProjects()
            .enqueue(object : Callback<List<ProjectResponse>> {
                override fun onFailure(call: Call<List<ProjectResponse>>, t: Throwable) {
                    if (context.isNetworkAvailable()) {
                        Toast.makeText(context, R.string.error_has_occurred, Toast.LENGTH_SHORT)
                            .show()
                    }
                    _binding?.projectSwipeRefreshView?.isRefreshing = false
                }

                override fun onResponse(
                    call: Call<List<ProjectResponse>>,
                    response: Response<List<ProjectResponse>>
                ) {
                    response.body()?.forEach { item ->
                        projectGroupDb.insertOrUpdate(item)
                    }
                    deletedProjectsFromCore(context)
                }
            })
    }

    private fun deletedProjectsFromCore(context: Context) {
        ApiManager.getInstance().getDeviceApi2(context).getDeletedProjects()
            .enqueue(object : Callback<List<ProjectResponse>> {
                override fun onFailure(call: Call<List<ProjectResponse>>, t: Throwable) {
                    if (context.isNetworkAvailable()) {
                        Toast.makeText(context, R.string.error_has_occurred, Toast.LENGTH_SHORT)
                            .show()
                    }
                }

                override fun onResponse(
                    call: Call<List<ProjectResponse>>,
                    response: Response<List<ProjectResponse>>
                ) {
                    if (response.isSuccessful) {
                        response.body()?.let { projectsRes ->
                            projectGroupDb.deleteProjectsByCoreId(projectsRes) // remove project with these coreIds
                        }
                        _binding?.projectSwipeRefreshView?.isRefreshing = false
                    }
                }
            })
    }

    override fun onClicked(project: Project) {
        projectProtocol?.onProjectClick(project)
    }

    override fun onLockImageClicked() {
        Toast.makeText(context, R.string.not_have_permission, Toast.LENGTH_SHORT).show()
    }

    override fun onRefresh() {
        retrieveProjects(requireContext())
        binding.projectSwipeRefreshView.isRefreshing = true
    }

    override fun onDestroy() {
        super.onDestroy()
        locationGroupLiveData.removeObserver(locationGroupObserve)
    }

    companion object {
        private const val ARG_PROJECT = "ARG_PROJECT"

        @JvmStatic
        fun newInstance(group: String?, screen: String?) = ProjectFragment().apply {
            arguments = Bundle().apply {
                putString(ARG_PROJECT, group)
                putString(ProjectActivity.EXTRA_SCREEN, screen)
            }
        }
    }
}
