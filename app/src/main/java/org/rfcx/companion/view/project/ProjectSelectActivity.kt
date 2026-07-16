package org.rfcx.companion.view.project

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import org.rfcx.companion.MainActivity
import org.rfcx.companion.R
import org.rfcx.companion.base.ViewModelFactory
import org.rfcx.companion.databinding.ActivityProjectSelectBinding
import org.rfcx.companion.entity.Project
import org.rfcx.companion.repo.api.CoreApiHelper
import org.rfcx.companion.repo.api.CoreApiServiceImpl
import org.rfcx.companion.repo.api.DeviceApiHelper
import org.rfcx.companion.repo.api.DeviceApiServiceImpl
import org.rfcx.companion.repo.local.LocalDataHelper
import org.rfcx.companion.service.DeploymentCleanupWorker
import org.rfcx.companion.util.*
import org.rfcx.companion.view.profile.locationgroup.ProjectListener
import org.rfcx.companion.view.project.viewmodel.ProjectSelectViewModel

class ProjectSelectActivity :
    AppCompatActivity(),
    ProjectListener,
    SwipeRefreshLayout.OnRefreshListener {

    private lateinit var projectSelectViewModel: ProjectSelectViewModel
    private val projectSelectAdapter by lazy { ProjectSelectAdapter(this) }

    private val analytics by lazy { Analytics(this) }

    private val preferences by lazy { Preferences.getInstance(this) }

    private var selectedProject = -1

    private lateinit var binding: ActivityProjectSelectBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProjectSelectBinding.inflate(layoutInflater)
        setContentView(binding.root)

        try {
            if (preferences.getInt(Preferences.SELECTED_PROJECT) != -1) {
                MainActivity.startActivity(this)
                finish()
            }
        } catch (e: Exception) {
            if (e is ClassCastException) {
                preferences.putInt(Preferences.SELECTED_PROJECT, -1)
            }
        }

        setViewModel()
        if (this.isNetworkAvailable()) {
            setObserver()
        } else {
            showToast(getString(R.string.network_not_available))
            addProjectsToAdapter()
        }

        binding.projectSwipeRefreshView.apply {
            setOnRefreshListener(this@ProjectSelectActivity)
            setColorSchemeResources(R.color.colorPrimary)
        }

        binding.projectView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = projectSelectAdapter
        }

        binding.selectProjectButton.setOnClickListener {
            preferences.putInt(Preferences.SELECTED_PROJECT, selectedProject)
            MainActivity.startActivity(this)
            finish()
        }

        binding.logoutButton.setOnClickListener {
            DeploymentCleanupWorker.stopAllWork(this)
            this.logout()
            LocationTrackingManager.set(this, false)
            analytics.trackLogoutEvent()
            finish()
        }
    }

    private fun setViewModel() {
        projectSelectViewModel = ViewModelProvider(
            this,
            ViewModelFactory(
                application,
                DeviceApiHelper(DeviceApiServiceImpl(this)),
                CoreApiHelper(CoreApiServiceImpl(this)),
                LocalDataHelper()
            )
        ).get(ProjectSelectViewModel::class.java)
    }

    private fun setObserver() {
        projectSelectViewModel.getProjectsFromRemote().observe(
            this,
            Observer {
                when (it.status) {
                    Status.LOADING -> {
                        showLoading()
                    }
                    Status.SUCCESS -> {
                        hideLoading()
                        it.data?.let { projects ->
                            if (projects.isEmpty()) {
                                binding.noContentTextView.visibility = View.VISIBLE
                            } else {
                                binding.noContentTextView.visibility = View.GONE
                            }
                        }
                        addProjectsToAdapter()
                    }
                    Status.ERROR -> {
                        hideLoading()
                        addProjectsToAdapter()
                        showToast(it.message ?: getString(R.string.error_has_occurred))
                    }
                }
            }
        )
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }

    private fun addProjectsToAdapter() {
        projectSelectAdapter.items = projectSelectViewModel.getProjectsFromLocal()
    }

    private fun showLoading() {
        binding.noContentTextView.visibility = View.GONE
        binding.projectSwipeRefreshView.isRefreshing = true
    }

    private fun hideLoading() {
        binding.projectSwipeRefreshView.isRefreshing = false
    }

    override fun onRefresh() {
        projectSelectViewModel.refreshProjects()
    }

    companion object {
        fun startActivity(context: Context) {
            val intent = Intent(context, ProjectSelectActivity::class.java)
            context.startActivity(intent)
        }
    }

    override fun onClicked(project: Project) {
        selectedProject = project.id
        binding.selectProjectButton.isEnabled = true
    }

    override fun onLockImageClicked() {
        Toast.makeText(this, R.string.not_have_permission, Toast.LENGTH_LONG).show()
    }
}
