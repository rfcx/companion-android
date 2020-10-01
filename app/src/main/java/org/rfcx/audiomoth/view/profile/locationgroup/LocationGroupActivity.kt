package org.rfcx.audiomoth.view.profile.locationgroup

import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.android.synthetic.main.activity_location_group.*
import kotlinx.android.synthetic.main.toolbar_default.*
import org.rfcx.audiomoth.R
import org.rfcx.audiomoth.view.detail.DeploymentImageAdapter
import org.rfcx.audiomoth.view.profile.coordinates.CoordinatesActivity

class LocationGroupActivity : AppCompatActivity() {
    private val locationGroupAdapter by lazy { LocationGroupAdapter() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_location_group)

        setupToolbar()

        locationGroupRecyclerView.apply {
            adapter = locationGroupAdapter
            layoutManager = LinearLayoutManager(context)
        }

        locationGroupAdapter.items = listOf("Location Group 01", "Location Group 02", "Location Group 03")
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
        fun startActivity(context: Context) {
            val intent = Intent(context, LocationGroupActivity::class.java)
            context.startActivity(intent)
        }
    }
}
