package org.rfcx.audiomoth.view.detail

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import org.rfcx.audiomoth.R

class EditLocationActivity : AppCompatActivity() {
    private var latitude: Double = 0.0
    private var longitude: Double = 0.0
    private var nameLocation: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_location)
        initIntent()

        Log.d("initIntent", "$latitude")
        Log.d("initIntent", "$longitude")
        Log.d("initIntent", "$nameLocation")
    }

    private fun initIntent() {
        intent.extras?.let {
            latitude = it.getDouble(EXTRA_LATITUDE)
            longitude = it.getDouble(EXTRA_LONGITUDE)
            nameLocation = it.getString(EXTRA_LOCATION_NAME)
        }
    }

    companion object {
        const val EXTRA_LATITUDE = "EXTRA_LATITUDE"
        const val EXTRA_LONGITUDE = "EXTRA_LONGITUDE"
        const val EXTRA_LOCATION_NAME = "EXTRA_LOCATION_NAME"

        fun startActivity(context: Context, lat: Double, lng: Double, name: String) {
            val intent = Intent(context, EditLocationActivity::class.java)
            intent.putExtra(EXTRA_LATITUDE, lat)
            intent.putExtra(EXTRA_LONGITUDE, lng)
            intent.putExtra(EXTRA_LOCATION_NAME, name)
            context.startActivity(intent)
        }
    }
}
