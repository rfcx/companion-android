package org.rfcx.audiomoth.view.dashboard

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import org.rfcx.audiomoth.R

class DashboardStreamActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dashboard)
    }

    companion object {
        fun startActivity(context: Context) {
            val intent = Intent(context, DashboardStreamActivity::class.java)
            context.startActivity(intent)
        }
    }
}
