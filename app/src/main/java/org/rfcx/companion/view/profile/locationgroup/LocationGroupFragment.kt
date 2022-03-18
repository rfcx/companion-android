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
import androidx.lifecycle.Transformations
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import io.realm.Realm
import kotlinx.android.synthetic.main.activity_project_select.*
import kotlinx.android.synthetic.main.fragment_location_group.*
import kotlinx.android.synthetic.main.fragment_location_group.projectSwipeRefreshView
import kotlinx.android.synthetic.main.fragment_map.*
import org.rfcx.companion.R
import org.rfcx.companion.entity.Project
import org.rfcx.companion.entity.response.ProjectResponse
import org.rfcx.companion.localdb.ProjectDb
import org.rfcx.companion.repo.ApiManager
import org.rfcx.companion.util.*
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class LocationGroupFragment :
    Fragment(),
    LocationGroupListener,
    SwipeRefreshLayout.OnRefreshListener {
    val realm: Realm = Realm.getInstance(RealmHelper.migrationConfig())
    private val locationGroupDb = ProjectDb(realm)

    private val locationGroupAdapter by lazy { LocationGroupAdapter(this) }
    private var locationGroupProtocol: LocationGroupProtocol? = null
    private var selectedGroup: String? = null
    private var screen: String? = null

    private lateinit var locationGroupLiveData: LiveData<List<Project>>
    private val locationGroupObserve = Observer<List<Project>> {
        locationGroupAdapter.apply {
            items = locationGroupDb.getProjects()
            notifyDataSetChanged()
        }
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        locationGroupProtocol = (context as LocationGroupProtocol)
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
        return inflater.inflate(R.layout.fragment_location_group, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        locationGroupRecyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = locationGroupAdapter
        }

        projectSwipeRefreshView.apply {
            setOnRefreshListener(this@LocationGroupFragment)
            setColorSchemeResources(R.color.colorPrimary)
        }

        locationGroupAdapter.apply {
            this.selectedGroup = selectedGroup
            this.screen = screen
        }

        getProjects()
    }

    private fun initIntent() {
        arguments?.let {
            selectedGroup = it.getString(ARG_GROUP)
            screen = it.getString(LocationGroupActivity.EXTRA_SCREEN)
        }
    }

    private fun getProjects() {
        locationGroupLiveData =
            Transformations.map(locationGroupDb.getAllResultsAsync().asLiveData()) {
                it
            }
        locationGroupLiveData.observeForever(locationGroupObserve)
    }

    private fun retrieveProjects(context: Context) {
        val token = "Bearer ${context.getIdToken()}"
        ApiManager.getInstance().getDeviceApi2().getProjects(token)
            .enqueue(object : Callback<List<ProjectResponse>> {
                override fun onFailure(call: Call<List<ProjectResponse>>, t: Throwable) {
                    if (context.isNetworkAvailable()) {
                        Toast.makeText(context, R.string.error_has_occurred, Toast.LENGTH_SHORT)
                            .show()
                    }
                    projectSwipeRefreshView.isRefreshing = false
                }

                override fun onResponse(
                    call: Call<List<ProjectResponse>>,
                    response: Response<List<ProjectResponse>>
                ) {
                    response.body()?.forEach { item ->
                        locationGroupDb.insertOrUpdate(item)
                    }
                    deletedProjectsFromCore(context)
                }
            })
    }

    private fun deletedProjectsFromCore(context: Context) {
        val token = "Bearer ${context.getIdToken()}"
        ApiManager.getInstance().getDeviceApi2().getDeletedProjects(token)
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
                            locationGroupDb.deleteProjectsByCoreId(projectsRes.map { it.id!! }) // remove project with these coreIds
                        }
                        projectSwipeRefreshView.isRefreshing = false
                    }
                }
            })
    }

    override fun onClicked(group: Project) {
        locationGroupProtocol?.onLocationGroupClick(group)
    }

    override fun onLockImageClicked() {
        Toast.makeText(context, R.string.not_have_permission, Toast.LENGTH_SHORT).show()
    }

    override fun onRefresh() {
        retrieveProjects(requireContext())
        projectSwipeRefreshView.isRefreshing = true
    }

    override fun onDestroy() {
        super.onDestroy()
        locationGroupLiveData.removeObserver(locationGroupObserve)
    }

    companion object {
        private const val ARG_GROUP = "ARG_GROUP"

        @JvmStatic
        fun newInstance(group: String?, screen: String?) = LocationGroupFragment().apply {
            arguments = Bundle().apply {
                putString(ARG_GROUP, group)
                putString(LocationGroupActivity.EXTRA_SCREEN, screen)
            }
        }
    }
}
