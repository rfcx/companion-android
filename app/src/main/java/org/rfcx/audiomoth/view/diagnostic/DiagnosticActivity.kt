package org.rfcx.audiomoth.view.diagnostic

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.Observer
import androidx.preference.PreferenceManager
import com.google.android.material.snackbar.Snackbar
import com.google.gson.JsonArray
import io.realm.Realm
import kotlinx.android.synthetic.main.activity_guardian_diagnostic.*
import kotlinx.android.synthetic.main.toolbar_default.*
import org.rfcx.audiomoth.R
import org.rfcx.audiomoth.connection.socket.SocketManager
import org.rfcx.audiomoth.entity.guardian.*
import org.rfcx.audiomoth.entity.socket.Status
import org.rfcx.audiomoth.localdb.guardian.DiagnosticDb
import org.rfcx.audiomoth.service.DiagnosticSyncWorker
import org.rfcx.audiomoth.util.RealmHelper
import org.rfcx.audiomoth.view.dialog.LoadingDialogFragment
import org.rfcx.audiomoth.view.prefs.GuardianPrefsFragment
import org.rfcx.audiomoth.view.prefs.SyncPreferenceListener

class DiagnosticActivity : AppCompatActivity(), SyncPreferenceListener {

    private val realm by lazy { Realm.getInstance(RealmHelper.migrationConfig()) }
    private val diagnosticDb: DiagnosticDb by lazy { DiagnosticDb(realm) }
    private val diagnosticInfo: DiagnosticInfo by lazy {
        diagnosticDb.getDiagnosticInfo(
            deploymentServerId
        )
    }

    private var collapseAdvanced = false
    private var prefsChanges: Map<String, String>? = null
    private var prefsEditor: SharedPreferences.Editor? = null

    private var switchPrefs = listOf(
        "show_ui",
        "enable_audio_capture",
        "enable_checkin_publish",
        "enable_cutoffs_battery",
        "enable_cutoffs_schedule_off_hours",
        "admin_enable_log_capture",
        "admin_enable_screenshot_capture",
        "admin_enable_bluetooth",
        "admin_enable_wifi",
        "admin_enable_tcp_adb",
        "admin_enable_sentinel_capture",
        "admin_enable_ssh_server",
        "admin_enable_wifi_socket"
    )

    private var lat: Double? = null
    private var long: Double? = null
    private var locationName: String? = null
    private var isConnected: Boolean? = null
    private var deployment: GuardianDeployment? = null
    private var deploymentServerId: String? = null
    private var configuration: GuardianConfiguration? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_guardian_diagnostic)
        getIntentExtra()
        setupToolbar()
        setupUiByConnection()
        setupLocationData()
        retrieveDiagnosticInfo()
        setupSyncButton()
    }

    private fun setupUiByConnection() {
        if (isConnected == false) {
            disableAllComponent(diagnosticScrollView)
        }
    }

    private fun getIntentExtra() {
        isConnected = intent.extras?.getBoolean(IS_CONNECTED)
        deployment = intent.extras?.getSerializable(DEPLOYMENT) as GuardianDeployment
        lat = deployment?.location?.latitude ?: 0.0
        long = deployment?.location?.longitude ?: 0.0
        locationName = deployment?.location?.name ?: ""
        deploymentServerId = deployment?.serverId ?: ""
        configuration = deployment?.configuration ?: GuardianConfiguration()
    }

    private fun setupAdvancedSetting() {
        val fragment = GuardianPrefsFragment()
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
                advanceCollapseIcon.background =
                    ContextCompat.getDrawable(this, R.drawable.ic_drop_down)
            }
        }
    }

    private fun setupToolbar() {
        setSupportActionBar(toolbar)
        supportActionBar?.apply {
            setDisplayShowTitleEnabled(true)
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowHomeEnabled(true)
            title =
                if (isConnected != false) locationName else "$locationName (${diagnosticInfo.getRelativeTimeSpan()})"
        }
    }

    private fun setupLocationData() {
        locationLongitudeValue.text = long.toString()
        locationLatitudeValue.text = lat.toString()
    }

    private fun retrieveDiagnosticInfo() {
        if (isConnected == true) {
            showLoading()
            SocketManager.getDiagnosticData()
            SocketManager.diagnostic.observe(this, Observer { diagnosticInfo ->
                val diagnosticData = diagnosticInfo.diagnostic
                val configurationData = diagnosticInfo.configure.toReadableFormat()
                val prefsData = diagnosticInfo.prefs

                runOnUiThread {
                    // set Diagnostic detail
                    detailRecordValue.text =
                        getString(R.string.detail_file, diagnosticData.totalLocal)
                    detailCheckInValue.text =
                        getString(R.string.detail_file, diagnosticData.totalCheckIn)
                    detailTotalSizeValue.text =
                        getString(R.string.detail_size, diagnosticData.totalFileSize / 1000)
                    detailTotalTimeValue.text = diagnosticData.getRecordTime()
                    detailBatteryValue.text =
                        getString(R.string.detail_percentage, diagnosticData.batteryPercentage)

                    // set Configuration
                    configFileFormatValue.text = configurationData.fileFormat
                    configSampleRateValue.text =
                        getString(R.string.detail_khz, configurationData.sampleRate)
                    configBitrateValue.text =
                        getString(R.string.detail_kbs, configurationData.bitrate)
                    configDurationValue.text =
                        getString(R.string.detail_secs, configurationData.duration)

                    setupAdvancedSetting()
                    setupCurrentPrefs(prefsData)
                    hideLoading()
                }

                saveNewDiagnostic()
            })

//                override fun onFailed(message: String) {
//                    runOnUiThread {
//                        hideLoading()
//                        Snackbar.make(
//                            diagRootView,
//                            "Getting diagnostic data failed",
//                            Snackbar.LENGTH_LONG
//                        )
//                            .setAction(R.string.retry) { retrieveDiagnosticInfo() }
//                            .show()
//                    }
//                }
        } else {
            setupLastKnownDiagnostic()
        }
    }

    private fun setupLastKnownDiagnostic() {
        // configuration data from marker
        val configurationInfo = configuration?.toReadableFormat()
        configFileFormatValue.text = configurationInfo?.fileFormat
        configSampleRateValue.text =
            getString(R.string.detail_khz, configurationInfo?.sampleRate)
        configBitrateValue.text =
            getString(R.string.detail_kbs, configurationInfo?.bitrate)
        configDurationValue.text =
            getString(R.string.detail_secs, configurationInfo?.duration)
    }

    private fun setupCurrentPrefs(prefs: JsonArray) {
        val prefsEditor = PreferenceManager.getDefaultSharedPreferences(this).edit()
        prefs.forEach {
            val pref = it.asJsonObject
            val key = ArrayList<String>(pref.keySet())[0]
            val value = pref.get(key).asString.replace("\"", "")
            if (switchPrefs.contains(key)) {
                prefsEditor.putBoolean(key, value.toBoolean()).apply()
            } else {
                prefsEditor.putString(key, value).apply()
            }
        }
    }

    private fun setupSyncButton() {
        syncButton.setOnClickListener {
            syncPrefs(prefsChanges!!)
        }
    }

    private fun showLoading() {
        val loadingDialog: LoadingDialogFragment =
            supportFragmentManager.findFragmentByTag(TAG_LOADING_DIALOG) as LoadingDialogFragment?
                ?: run {
                    LoadingDialogFragment()
                }
        loadingDialog.show(supportFragmentManager, TAG_LOADING_DIALOG)
    }

    private fun hideLoading() {
        val loadingDialog: LoadingDialogFragment? =
            supportFragmentManager.findFragmentByTag(TAG_LOADING_DIALOG) as LoadingDialogFragment?
        loadingDialog?.dismissDialog()
    }

    private fun disableAllComponent(view: View) {
        view.isEnabled = false
        if (view is ViewGroup) {
            for (i in 0 until view.childCount) {
                val child = view.getChildAt(i)
                child.alpha = 0.7f
                disableAllComponent(child)
            }
        }
    }

    private fun saveNewDiagnostic() {
        diagnosticDb.insertOrUpdate(this.deploymentServerId)
        DiagnosticSyncWorker.enqueue(this)
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

            SocketManager.syncConfiguration(listForGuardian)
            SocketManager.syncConfiguration.observe(this, Observer { syncConfiguration ->
                if (syncConfiguration.sync.status == Status.SUCCESS.value) {
                    showSuccessResponse()
                } else {
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

    override fun setEditor(editor: SharedPreferences.Editor) {
        this.prefsEditor = editor
    }

    override fun onBackPressed() {
        super.onBackPressed()
        finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        this.prefsEditor?.clear()?.apply()
    }

    companion object {
        private const val TAG_LOADING_DIALOG = "TAG_LOADING_DIALOG"
        const val IS_CONNECTED = "is_connected"
        const val DEPLOYMENT = "deployment"

        fun startActivity(context: Context) {
            val intent = Intent(context, DiagnosticActivity::class.java)
            context.startActivity(intent)
        }

        fun startActivity(context: Context, deployment: GuardianDeployment, isConnected: Boolean) {
            val intent = Intent(context, DiagnosticActivity::class.java)
            intent.putExtra(IS_CONNECTED, isConnected)
            intent.putExtra(DEPLOYMENT, deployment)
            context.startActivity(intent)
        }
    }
}
