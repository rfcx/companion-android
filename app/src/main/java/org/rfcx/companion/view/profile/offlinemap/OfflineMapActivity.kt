package org.rfcx.companion.view.profile.offlinemap

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import kotlinx.android.synthetic.main.activity_offline_map.*
import kotlinx.android.synthetic.main.toolbar_default.*
import org.rfcx.companion.R

class OfflineMapActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_offline_map)
        setupToolbar()

        startFragment(OfflineMapFragment.newInstance())
    }

    private fun setupToolbar() {
        setSupportActionBar(toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowHomeEnabled(true)
            title = getString(R.string.profile_offline_map_label)
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }

    private fun startFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(offlineMapContainer.id, fragment)
            .commit()
    }

    companion object {
        fun startActivity(context: Context) {
            val intent = Intent(context, OfflineMapActivity::class.java)
            context.startActivity(intent)
        }
    }
}
