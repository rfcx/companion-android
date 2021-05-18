package org.rfcx.companion.view.project

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import io.realm.Realm
import kotlinx.android.synthetic.main.activity_project_select.*
import org.rfcx.companion.MainActivity
import org.rfcx.companion.R
import org.rfcx.companion.entity.response.ProjectResponse
import org.rfcx.companion.localdb.ProjectDb
import org.rfcx.companion.repo.ApiManager
import org.rfcx.companion.util.Preferences
import org.rfcx.companion.util.RealmHelper
import org.rfcx.companion.util.getIdToken
import org.rfcx.companion.util.isNetworkAvailable
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class ProjectSelectActivity : AppCompatActivity(), (Int) -> Unit, SwipeRefreshLayout.OnRefreshListener {

    private val realm by lazy { Realm.getInstance(RealmHelper.migrationConfig()) }
    private val projectDb by lazy { ProjectDb(realm) }

    private val preferences by lazy { Preferences.getInstance(this) }

    private val projectSelectAdapter by lazy { ProjectSelectAdapter(this) }

    private var selectedProject = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_project_select)

        if (preferences.getInt(Preferences.SELECTED_PROJECT) != -1) {
            MainActivity.startActivity(this)
            finish()
        }

        projectSwipeRefreshView.apply {
            setOnRefreshListener(this@ProjectSelectActivity)
            setColorSchemeResources(R.color.colorPrimary)
        }

        downloadAccessibleProjects(this)

        projectView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = projectSelectAdapter
        }

        selectProjectButton.setOnClickListener {
            preferences.putInt(Preferences.SELECTED_PROJECT, selectedProject)
            MainActivity.startActivity(this)
            finish()
        }

    }

    private fun addProjectsToAdapter() {
        projectSelectAdapter.items = projectDb.getProjects()
    }

    private fun downloadAccessibleProjects(context: Context) {
        showLoading()
        val token = "Bearer ${context.getIdToken()}"
        ApiManager.getInstance().getDeviceApi().getProjects(token)
            .enqueue(object : Callback<List<ProjectResponse>> {
                override fun onFailure(call: Call<List<ProjectResponse>>, t: Throwable) {
                    if (context.isNetworkAvailable()) {
                        Toast.makeText(context, t.message, Toast.LENGTH_SHORT)
                            .show()
                    }
                }

                override fun onResponse(
                    call: Call<List<ProjectResponse>>,
                    response: Response<List<ProjectResponse>>
                ) {
                    response.body()?.let { projects ->
                        projects.forEach { item ->
                            projectDb.insertOrUpdate(item)
                        }
                    }
                    hideLoading()
                    addProjectsToAdapter()
                }
            })
    }

    private fun showLoading() {
        projectSwipeRefreshView.isRefreshing = true
    }

    private fun hideLoading() {
        projectSwipeRefreshView.isRefreshing = false
    }

    override fun invoke(id: Int) {
        selectedProject = id
        selectProjectButton.isEnabled = true
    }

    override fun onRefresh() {
        downloadAccessibleProjects(this)
    }

    companion object {
        fun startActivity(context: Context) {
            val intent = Intent(context, ProjectSelectActivity::class.java)
            context.startActivity(intent)
        }
    }
}
