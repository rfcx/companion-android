package org.rfcx.audiomoth.view.diagnostic

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.material.snackbar.Snackbar
import com.google.gson.JsonArray
import kotlinx.android.synthetic.main.activity_guardian_diagnostic.*
import org.rfcx.audiomoth.R
import org.rfcx.audiomoth.connection.socket.OnReceiveResponse
import org.rfcx.audiomoth.connection.socket.SocketManager
import org.rfcx.audiomoth.entity.guardian.toReadableFormat
import org.rfcx.audiomoth.entity.socket.DiagnosticResponse
import org.rfcx.audiomoth.entity.socket.SocketResposne
import org.rfcx.audiomoth.entity.socket.getRecordTime
import org.rfcx.audiomoth.view.deployment.guardian.GuardianDeploymentActivity
import org.rfcx.audiomoth.view.dialog.LoadingDialogFragment
import org.rfcx.audiomoth.view.prefs.GuardianPrefsFragment
import org.rfcx.audiomoth.view.prefs.SyncPreferenceListener

class DiagnosticActivity : AppCompatActivity(), SyncPreferenceListener {

    private var collapseAdvanced = false
    private var prefsChanges: Map<String, String>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_guardian_diagnostic)
        setupToolbar()
        setupLocationData()
        retrieveDiagnosticInfo()
        setupSyncButton()
    }

    private fun setupAdvancedSetting(prefs: JsonArray) {
        val fragment = GuardianPrefsFragment(prefs)
        diagnosticAdvanceLayout.setOnClickListener {
            if (!collapseAdvanced) {
                supportFragmentManager.beginTransaction()
                    .replace(advancedContainer.id, fragment)
                    .commit()
                collapseAdvanced = true
                advanceCollapseIcon.background = ContextCompat.getDrawable(this, R.drawable.ic_up)
            } else {
                supportFragmentManager.beginTransaction()
                    .remove(fragment)
                    .commit()
                collapseAdvanced = false
                advanceCollapseIcon.background = ContextCompat.getDrawable(this, R.drawable.ic_drop_down)
            }
        }
    }

    private fun setupToolbar() {
        //TODO: waiting for pin feature work, so we can use location name here
        setSupportActionBar(diagnosticToolbar)
        supportActionBar?.apply {
            setDisplayShowTitleEnabled(true)
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowHomeEnabled(true)
            title = "Location1"
        }
    }

    private fun setupLocationData() {
        //TODO: waiting for pin feature work, so we can use latitude longitude here
    }

    private fun retrieveDiagnosticInfo() {
        showLoading()
        SocketManager.getDiagnosticData(object : OnReceiveResponse {
            override fun onReceive(response: SocketResposne) {
                val diagnosticInfo = response as DiagnosticResponse
                val diagnosticData = diagnosticInfo.diagnostic
                val configurationData = diagnosticInfo.configure.toReadableFormat()
                val prefsData = diagnosticInfo.prefs

                runOnUiThread {
                    //set Diagnostic detail
                    detailRecordValue.text =
                        getString(R.string.detail_file, diagnosticData.totalLocal)
                    detailCheckInValue.text =
                        getString(R.string.detail_file, diagnosticData.totalCheckIn)
                    detailTotalSizeValue.text =
                        getString(R.string.detail_size, diagnosticData.totalFileSize / 1000)
                    detailTotalTimeValue.text = diagnosticData.getRecordTime()
                    detailBatteryValue.text =
                        getString(R.string.detail_percentage, diagnosticData.batteryPercentage)

                    //set Configuration
                    configFileFormatValue.text = configurationData.fileFormat
                    configSampleRateValue.text =
                        getString(R.string.detail_khz, configurationData.sampleRate)
                    configBitrateValue.text =
                        getString(R.string.detail_kbs, configurationData.bitrate)
                    configDurationValue.text =
                        getString(R.string.detail_secs, configurationData.duration)

                    setupAdvancedSetting(prefsData)
                    hideLoading()
                }
            }

            override fun onFailed(message: String) {
                runOnUiThread {
                    hideLoading()
                }
            }

        })
    }

    private fun setupSyncButton() {
        syncButton.setOnClickListener {
            syncPrefs(prefsChanges!!)
        }
    }

    private fun showLoading() {
        val loadingDialog: LoadingDialogFragment =
            supportFragmentManager.findFragmentByTag(GuardianDeploymentActivity.loadingDialogTag) as LoadingDialogFragment?
                ?: run {
                    LoadingDialogFragment()
                }
        loadingDialog.show(supportFragmentManager, GuardianDeploymentActivity.loadingDialogTag)
    }

    private fun hideLoading() {
        val loadingDialog: LoadingDialogFragment? =
            supportFragmentManager.findFragmentByTag(GuardianDeploymentActivity.loadingDialogTag) as LoadingDialogFragment?
        loadingDialog?.dismissDialog()
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }

    override fun setPrefsChanges(prefs: Map<String, String>) {
       this.prefsChanges = prefs
    }

    override fun showSyncButton() {
        syncButton.visibility = View.VISIBLE
    }

    override fun hideSyncButton() {
        syncButton.visibility = View.INVISIBLE
    }

    override fun syncPrefs(prefs: Map<String, String>) {
        if (prefs.isNotEmpty()) {
            val listForGuardian = mutableListOf<String>()
            prefs.forEach {
                listForGuardian.add("${it.key}|${it.value}")
            }

            SocketManager.syncConfiguration(listForGuardian, object : OnReceiveResponse {
                override fun onReceive(response: SocketResposne) {
                    showSuccessResponse()
                }

                override fun onFailed(message: String) {
                    showFailedResponse()
                }
            })

            hideSyncButton()
        }
    }

    override fun showSuccessResponse() {
        Snackbar.make(diagRootView, "Sync preferences success", Snackbar.LENGTH_LONG)
            .show()
    }

    override fun showFailedResponse() {
        Snackbar.make(diagRootView, "Sync preferences failed", Snackbar.LENGTH_LONG)
            .setAction(R.string.retry) { syncPrefs(prefsChanges ?: mapOf()) }
            .show()
    }

    override fun onBackPressed() {
        super.onBackPressed()
        finish()
    }


    companion object {
        fun startActivity(context: Context) {
            val intent = Intent(context, DiagnosticActivity::class.java)
            context.startActivity(intent)
        }
    }
}
