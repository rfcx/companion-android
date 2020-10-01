package org.rfcx.audiomoth.view.profile.locationgroup

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.toolbar_default.*
import org.rfcx.audiomoth.R

class CreateNewGroupActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create_new_group)
        setupToolbar()
    }

    private fun setupToolbar() {
        setSupportActionBar(toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowHomeEnabled(true)
            title = getString(R.string.create_new_group)
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }

    companion object {
        fun startActivity(context: Context) {
            val intent = Intent(context, CreateNewGroupActivity::class.java)
            context.startActivity(intent)
        }
    }
}
