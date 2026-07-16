package org.rfcx.companion.view.profile.offlinemap

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import org.rfcx.companion.R
import org.rfcx.companion.databinding.ActivityOfflineMapBinding

class OfflineMapActivity : AppCompatActivity() {
    private lateinit var binding: ActivityOfflineMapBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityOfflineMapBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupToolbar()

        startFragment(OfflineMapFragment.newInstance())
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbarLayout.toolbar)
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
            .replace(binding.offlineMapContainer.id, fragment)
            .commit()
    }

    companion object {
        fun startActivity(context: Context) {
            val intent = Intent(context, OfflineMapActivity::class.java)
            context.startActivity(intent)
        }
    }
}
