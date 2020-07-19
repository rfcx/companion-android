package org.rfcx.audiomoth.view.profile.coordinates

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_coordinates.*
import kotlinx.android.synthetic.main.toolbar_default.*
import org.rfcx.audiomoth.R
import org.rfcx.audiomoth.util.Preferences
import org.rfcx.audiomoth.util.getCoordinatesFormat

class CoordinatesActivity : AppCompatActivity() {
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
