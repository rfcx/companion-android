package org.rfcx.companion.view.profile.locationgroup

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import io.realm.Realm
import kotlinx.android.synthetic.main.activity_location_group.*
import kotlinx.android.synthetic.main.toolbar_default.*
import org.rfcx.companion.R
import org.rfcx.companion.entity.LocationGroups
import org.rfcx.companion.entity.Screen
import org.rfcx.companion.entity.toLocationGroup
import org.rfcx.companion.localdb.DatabaseCallback
import org.rfcx.companion.localdb.EdgeDeploymentDb
import org.rfcx.companion.service.DeploymentSyncWorker
import org.rfcx.companion.util.Preferences
import org.rfcx.companion.util.RealmHelper
import org.rfcx.companion.util.showCommonDialog
import org.rfcx.companion.view.BaseActivity

class LocationGroupActivity : BaseActivity(), LocationGroupProtocol {

    // For detail page to edit location group
    private val realm by lazy { Realm.getInstance(RealmHelper.migrationConfig()) }
    private val edgeDeploymentDb by lazy { EdgeDeploymentDb(realm) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_location_group)

        setupToolbar()

        val group: String? = intent?.getStringExtra(EXTRA_GROUP)
        startFragment(LocationGroupFragment.newInstance(group))
    }

    override fun onCreateNewGroup() {
        CreateNewGroupActivity.startActivity(this)
    }

    override fun onLocationGroupClick(group: LocationGroups) {
        val screen: String? = intent?.getStringExtra(EXTRA_SCREEN)
        when(screen) {
            Screen.LOCATION.id -> {
                val preferences = Preferences.getInstance(this)
                preferences.putString(Preferences.GROUP, group.name)
                finish()
            }
            Screen.EDGE_DETAIL.id -> {
                val deploymentId: Int? = intent.extras?.getInt(EXTRA_DEPLOYMENT_ID)
                deploymentId?.let { id ->
                    showLoading()
                    edgeDeploymentDb.editLocationGroup(id, group.toLocationGroup(), object :
                        DatabaseCallback {
                        override fun onSuccess() {
                            hideLoading()
                            DeploymentSyncWorker.enqueue(this@LocationGroupActivity)
                            finish()
                        }

                        override fun onFailure(errorMessage: String) {
                            hideLoading()
                            showCommonDialog(errorMessage)
                        }
                    })
                }
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

    companion object {
        const val EXTRA_GROUP = "EXTRA_GROUP"
        const val EXTRA_SCREEN = "EXTRA_SCREEN"
        const val EXTRA_DEPLOYMENT_ID = "EXTRA_DEPLOYMENT_ID"

        fun startActivity(
            context: Context,
            group: String? = null,
            screen: String = Screen.PROFILE.id
        ) {
            val intent = Intent(context, LocationGroupActivity::class.java)
            if (group != null)
                intent.putExtra(EXTRA_GROUP, group)
            intent.putExtra(EXTRA_SCREEN, screen)
            context.startActivity(intent)
        }

        fun startActivity(
            context: Context,
            group: String? = null,
            screen: String = Screen.PROFILE.id,
            requestCode: Int
        ) {
            val intent = Intent(context, LocationGroupActivity::class.java)
            if (group != null)
                intent.putExtra(EXTRA_GROUP, group)
            intent.putExtra(EXTRA_SCREEN, screen)
            (context as Activity).startActivityForResult(intent, requestCode)
        }

        fun startActivity(
            context: Context,
            group: String? = null,
            deploymentId: Int? = null,
            screen: String = Screen.EDGE_DETAIL.id,
            requestCode: Int
        ) {
            val intent = Intent(context, LocationGroupActivity::class.java)
            if (group != null)
                intent.putExtra(EXTRA_GROUP, group)
            if (deploymentId != null)
                intent.putExtra(EXTRA_DEPLOYMENT_ID, deploymentId)
            intent.putExtra(EXTRA_SCREEN, screen)
            (context as Activity).startActivityForResult(intent, requestCode)
        }
    }
}
