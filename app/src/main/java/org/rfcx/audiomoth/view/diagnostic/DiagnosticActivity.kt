package org.rfcx.audiomoth.view.diagnostic

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_guardian_diagnostic.*
import org.rfcx.audiomoth.R
import org.rfcx.audiomoth.view.prefs.GuardianPrefsFragment

class DiagnosticActivity : AppCompatActivity() {

    private var collapseAdvanced = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_guardian_diagnostic)
        setupToolbar()
        setupAdvancedSetting()
    }

    private fun setupAdvancedSetting() {
        val fragment = GuardianPrefsFragment()
        diagnosticAdvanceLayout.setOnClickListener {
            if (!collapseAdvanced) {
                supportFragmentManager.beginTransaction()
                    .replace(advancedContainer.id, fragment)
                    .commit()
                collapseAdvanced = true
            } else {
                supportFragmentManager.beginTransaction()
                    .remove(fragment)
                    .commit()
                collapseAdvanced = false
            }
        }
    }

    private fun setupToolbar() {
        setSupportActionBar(diagnosticToolbar)
        supportActionBar?.apply {
            setDisplayShowTitleEnabled(true)
            title = "Location1"
        }
    }

    private fun retrieveDiagnosticInfo() {

    }


    companion object {
        fun startActivity(context: Context) {
            val intent = Intent(context, DiagnosticActivity::class.java)
            context.startActivity(intent)
        }
    }
}
