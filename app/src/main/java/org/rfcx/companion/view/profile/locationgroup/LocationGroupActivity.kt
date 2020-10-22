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
import org.rfcx.companion.localdb.EdgeDeploymentDb
import org.rfcx.companion.util.Preferences
import org.rfcx.companion.util.RealmHelper
import org.rfcx.companion.view.BaseActivity
import org.rfcx.companion.view.detail.EditLocationActivity

class LocationGroupActivity : BaseActivity(), LocationGroupProtocol {

    // For detail page to edit location group
    private val realm by lazy { Realm.getInstance(RealmHelper.migrationConfig()) }
    private val edgeDeploymentDb by lazy { EdgeDeploymentDb(realm) }

    private var group: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_location_group)

        setupToolbar()

        group = intent?.getStringExtra(EXTRA_GROUP)
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
            Screen.EDIT_LOCATION.id -> {
                val intent = Intent()
                intent.putExtra(EditLocationActivity.EXTRA_LOCATION_GROUP, group.toLocationGroup())
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
        intent.putExtra(EXTRA_GROUP, group)
        setResult(RESULT_DELETE, intent)
        finish()
    }

    companion object {
        const val EXTRA_GROUP = "EXTRA_GROUP"
        const val EXTRA_SCREEN = "EXTRA_SCREEN"
        const val EXTRA_DEPLOYMENT_ID = "EXTRA_DEPLOYMENT_ID"
        const val RESULT_OK = 1
        const val RESULT_DELETE = 2

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
