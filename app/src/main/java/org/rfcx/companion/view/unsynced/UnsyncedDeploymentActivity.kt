package org.rfcx.companion.view.unsynced

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import org.rfcx.companion.R

class UnsyncedDeploymentActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_unsynced_deployment)
    }

    companion object {
        fun startActivity(context: Context) {
            val intent = Intent(context, UnsyncedDeploymentActivity::class.java)
            context.startActivity(intent)
        }
    }
}
