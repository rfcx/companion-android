package org.rfcx.audiomoth.view.configure

import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import kotlinx.android.synthetic.main.activity_configure.*
import org.rfcx.audiomoth.R

class ConfigureActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_configure)

        supportFragmentManager.beginTransaction()
            .add(configureContainer.id, ConfigureFragment(),
                "ConfigureFragment").commit()
    }

    companion object {
        fun startActivity(context: Context) {
            val intent = Intent(context, ConfigureActivity::class.java)
            context.startActivity(intent)
        }
    }
}
