package org.rfcx.companion.view.profile.coordinates

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_coordinates.*
import kotlinx.android.synthetic.main.toolbar_default.*
import org.rfcx.companion.R
import org.rfcx.companion.entity.Screen
import org.rfcx.companion.util.Analytics
import org.rfcx.companion.util.Preferences
import org.rfcx.companion.util.getCoordinatesFormat

class CoordinatesActivity : AppCompatActivity() {
    private val analytics by lazy { Analytics(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_coordinates)
        setupToolbar()

        val preferences = Preferences.getInstance(this)
        this.getCoordinatesFormat()?.let { showChecker(it) }

        ddLayout.setOnClickListener {
            preferences.putString(Preferences.COORDINATES_FORMAT, DD_FORMAT)
            showChecker(DD_FORMAT)
            finish()
        }

        ddmLayout.setOnClickListener {
            preferences.putString(Preferences.COORDINATES_FORMAT, DDM_FORMAT)
            showChecker(DDM_FORMAT)
            finish()
        }

        dmsLayout.setOnClickListener {
            preferences.putString(Preferences.COORDINATES_FORMAT, DMS_FORMAT)
            showChecker(DMS_FORMAT)
            finish()
        }
    }

    private fun showChecker(format: String) {
        analytics.trackChangeCoordinatesEvent(format)
        when (format) {
            DD_FORMAT -> {
                checkDDImageView.visibility = View.VISIBLE
                checkDDMImageView.visibility = View.INVISIBLE
                checkDMSImageView.visibility = View.INVISIBLE
            }
            DDM_FORMAT -> {
                checkDDImageView.visibility = View.INVISIBLE
                checkDDMImageView.visibility = View.VISIBLE
                checkDMSImageView.visibility = View.INVISIBLE
            }
            DMS_FORMAT -> {
                checkDDImageView.visibility = View.INVISIBLE
                checkDDMImageView.visibility = View.INVISIBLE
                checkDMSImageView.visibility = View.VISIBLE
            }
        }
    }

    private fun setupToolbar() {
        setSupportActionBar(toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowHomeEnabled(true)
            title = getString(R.string.coordinates)
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }

    override fun onResume() {
        super.onResume()
        analytics.trackScreen(Screen.COORDINATES)
    }

    companion object {
        const val DD_FORMAT = "DD"
        const val DDM_FORMAT = "DDM"
        const val DMS_FORMAT = "DMS"

        fun startActivity(context: Context) {
            val intent = Intent(context, CoordinatesActivity::class.java)
            context.startActivity(intent)
        }
    }
}
