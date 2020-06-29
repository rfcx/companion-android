package org.rfcx.audiomoth.view.prefs

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceScreen
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.android.synthetic.main.activity_deployment.*
import kotlinx.android.synthetic.main.activity_guardian_preferences.*
import org.rfcx.audiomoth.R
import org.rfcx.audiomoth.view.deployment.DeploymentActivity

class GuardianPrefsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_guardian_preferences)

        supportFragmentManager.beginTransaction()
            .replace(prefContainer.id, GuardianPrefsFragment())
            .commit()
    }

    override fun onBackPressed() {
        super.onBackPressed()
        finish()
    }

    companion object {
        fun startActivity(context: Context) {
            val intent = Intent(context, GuardianPrefsActivity::class.java)
            context.startActivity(intent)
        }
    }
}

