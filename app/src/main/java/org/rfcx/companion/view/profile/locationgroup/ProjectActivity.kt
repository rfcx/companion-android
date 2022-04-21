package org.rfcx.companion.view.profile.locationgroup

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import kotlinx.android.synthetic.main.activity_location_group.*
import kotlinx.android.synthetic.main.toolbar_default.*
import org.rfcx.companion.R
import org.rfcx.companion.entity.Project
import org.rfcx.companion.entity.Screen
import org.rfcx.companion.util.Preferences
import org.rfcx.companion.view.detail.EditLocationActivity

class ProjectActivity : AppCompatActivity(), ProjectProtocol {

    // For detail page to edit location group
    private var project: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_location_group)

        setupToolbar()

        val screen: String? = intent?.getStringExtra(EXTRA_SCREEN)
        project = intent?.getStringExtra(EXTRA_PROJECT)

        startFragment(ProjectFragment.newInstance(project, screen))
    }
    override fun onProjectClick(project: Project) {
        projectSelected(project.id)
    }

    private fun projectSelected(id: Int) {
        val screen: String? = intent?.getStringExtra(EXTRA_SCREEN)
        val preferences = Preferences.getInstance(this)
        when (screen) {
            Screen.LOCATION.id -> {
                preferences.putInt(Preferences.EDIT_PROJECT, id)
                finish()
            }
            Screen.DETAIL_DEPLOYMENT_SITE.id -> {
                preferences.putInt(Preferences.EDIT_PROJECT, id)
                finish()
            }
            Screen.EDIT_LOCATION.id -> {
                val intent = Intent()
                intent.putExtra(EditLocationActivity.EXTRA_PROJECT_ID, id)
                setResult(RESULT_OK, intent)
                finish()
            }
        }
    }

    private fun startFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(locationGroupContainer.id, fragment)
            .commit()
    }

    private fun setupToolbar() {
        setSupportActionBar(toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowHomeEnabled(true)
            title = getString(R.string.location_group)
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }

    override fun onBackPressed() {
        val intent = Intent()
        intent.putExtra(EXTRA_PROJECT, project)
        setResult(RESULT_DELETE, intent)
        finish()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == LOCATION_GROUP_REQUEST_CODE) {
            if (resultCode == RESULT_OK) {
                val id =
                    data?.getIntExtra(EditLocationActivity.EXTRA_PROJECT_ID, -1) ?: -1
                projectSelected(id)
            }
        }
    }

    companion object {
        const val EXTRA_PROJECT = "EXTRA_PROJECT"
        const val EXTRA_SCREEN = "EXTRA_SCREEN"
        const val EXTRA_DEPLOYMENT_ID = "EXTRA_DEPLOYMENT_ID"
        const val RESULT_OK = 1
        const val RESULT_DELETE = 2
        const val LOCATION_GROUP_REQUEST_CODE = 1004

        fun startActivity(
            context: Context,
            project: Int? = -1,
            screen: String = Screen.PROFILE.id
        ) {
            val intent = Intent(context, ProjectActivity::class.java)
            if (project != null)
                intent.putExtra(EXTRA_PROJECT, project)
            intent.putExtra(EXTRA_SCREEN, screen)
            context.startActivity(intent)
        }

        fun startActivity(
            context: Context,
            project: Int? = -1,
            deploymentId: Int? = null,
            screen: String = Screen.EDGE_DETAIL.id,
            requestCode: Int
        ) {
            val intent = Intent(context, ProjectActivity::class.java)
            if (project != null)
                intent.putExtra(EXTRA_PROJECT, project)
            if (deploymentId != null)
                intent.putExtra(EXTRA_DEPLOYMENT_ID, deploymentId)
            intent.putExtra(EXTRA_SCREEN, screen)
            (context as Activity).startActivityForResult(intent, requestCode)
        }
    }
}
