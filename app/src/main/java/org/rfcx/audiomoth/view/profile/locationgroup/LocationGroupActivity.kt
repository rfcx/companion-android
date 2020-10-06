package org.rfcx.audiomoth.view.profile.locationgroup

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import kotlinx.android.synthetic.main.activity_location_group.*
import kotlinx.android.synthetic.main.toolbar_default.*
import org.rfcx.audiomoth.R
import org.rfcx.audiomoth.entity.LocationGroups
import org.rfcx.audiomoth.entity.Screen
import org.rfcx.audiomoth.util.Preferences
import org.rfcx.audiomoth.view.deployment.locate.LocationFragment.Companion.LOCATION_RESULT_CODE

class LocationGroupActivity : AppCompatActivity(), LocationGroupProtocol {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_location_group)

        setupToolbar()
        startFragment(LocationGroupFragment.newInstance())

        val group: String? = intent?.getStringExtra(EXTRA_GROUP)
        val screen: String? = intent?.getStringExtra(EXTRA_SCREEN)
        // TODO: set group when seleced
    }

    override fun onCreateNewGroup() {
        CreateNewGroupActivity.startActivity(this)
    }

    override fun onLocationGroupClick(group: LocationGroups) {
        val preferences = Preferences.getInstance(this)
        preferences.putString(Preferences.GROUP, group.name)

        finish()
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
    }
}
