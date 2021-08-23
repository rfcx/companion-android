package org.rfcx.companion.view.profile.guardiansoftware

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import org.rfcx.companion.R

class GuardianSoftwareActivity: AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_guardian_software)
    }

    companion object {
        fun startActivity(context: Context) {
            val intent = Intent(context, GuardianSoftwareActivity::class.java)
            context.startActivity(intent)
        }
    }
}
