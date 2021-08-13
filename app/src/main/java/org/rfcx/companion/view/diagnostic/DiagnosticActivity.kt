package org.rfcx.companion.view.diagnostic

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.Transformations
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.snackbar.Snackbar
import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import io.realm.Realm
import kotlinx.android.synthetic.main.activity_guardian_diagnostic.*
import kotlinx.android.synthetic.main.toolbar_default.*
import org.rfcx.companion.BuildConfig
import org.rfcx.companion.R
import org.rfcx.companion.connection.socket.SocketManager
import org.rfcx.companion.entity.DeploymentImage
import org.rfcx.companion.entity.Device
import org.rfcx.companion.entity.Screen
import org.rfcx.companion.entity.guardian.GuardianConfiguration
import org.rfcx.companion.entity.guardian.GuardianDeployment
import org.rfcx.companion.entity.guardian.getRecordTime
import org.rfcx.companion.entity.guardian.toReadableFormat
import org.rfcx.companion.entity.socket.response.Status
import org.rfcx.companion.localdb.DeploymentImageDb
import org.rfcx.companion.localdb.ProjectDb
import org.rfcx.companion.localdb.guardian.GuardianDeploymentDb
import org.rfcx.companion.util.*
import org.rfcx.companion.view.detail.*
import org.rfcx.companion.view.dialog.LoadingDialogFragment
import org.rfcx.companion.view.prefs.GuardianPrefsFragment
import org.rfcx.companion.view.prefs.SyncPreferenceListener

class DiagnosticActivity : AppCompatActivity(), SyncPreferenceListener, (DeploymentImageView) -> Unit {

    private val realm by lazy { Realm.getInstance(RealmHelper.migrationConfig()) }
    private val locationGroupDb by lazy { ProjectDb(realm) }
    private val guardianDeploymentDb by lazy { GuardianDeploymentDb(realm) }
    private val deploymentImageDb by lazy { DeploymentImageDb(realm) }
    private val deploymentImageAdapter by lazy { DeploymentImageAdapter() }

    private lateinit var deployImageLiveData: LiveData<List<DeploymentImage>>
    private var deploymentImages = listOf<DeploymentImage>()
    private val deploymentImageObserve = Observer<List<DeploymentImage>> {
        deploymentImages = it
        updateDeploymentImages(deploymentImages)
    }

    private var collapseAdvanced = false
    private var prefsChanges: Map<String, String>? = null
    private var prefsEditor: SharedPreferences.Editor? = null

    private var switchPrefs = listOf<String>()

    private var lat: Double? = null
    private var long: Double? = null
    private var altitude: Double? = null
    private var locationName: String? = null
    private var isConnected: Boolean? = null
    private var deployment: GuardianDeployment? = null
    private var deploymentServerId: String? = null
    private var configuration: GuardianConfiguration? = null
    private val analytics by lazy { Analytics(this) }

    private var firstTimeEntered = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_guardian_diagnostic)
        getIntentExtra()
        setupToolbar()
        setupImageRecycler()
        setupUiByConnection()
        setupLocationData()
        retrieveDiagnosticInfo()
        setupSyncButton()
        setupEditLocationButton()
    }

    private fun setupUiByConnection() {
        if (isConnected == false) {
            disableAllComponent(diagnosticScrollView)
        }
    }

    private fun setupImageRecycler() {
        deploymentImageRecycler.apply {
            adapter = deploymentImageAdapter
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
            setHasFixedSize(true)
        }
        deployment?.id?.let { observeDeploymentImage(it) }
    }

    private fun getIntentExtra() {
        isConnected = intent.extras?.getBoolean(IS_CONNECTED)
        deployment = intent.extras?.getSerializable(DEPLOYMENT) as GuardianDeployment
        lat = deployment?.stream?.latitude ?: 0.0
        long = deployment?.stream?.longitude ?: 0.0
        altitude = deployment?.stream?.altitude ?: 0.0
        locationName = deployment?.stream?.name ?: ""
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

    private fun observeDeploymentImage(deploymentId: Int) {
        deployImageLiveData =
            Transformations.map(deploymentImageDb.getAllResultsAsync(deploymentId, device = Device.GUARDIAN.value).asLiveData()) {
                it
            }
        deployImageLiveData.observeForever(deploymentImageObserve)
    }

    private fun updateDeploymentImages(deploymentImages: List<DeploymentImage>) {
        photoTitle.visibility = if (deploymentImages.isNotEmpty()) View.VISIBLE else View.GONE
        separateLine5.visibility = if (deploymentImages.isNotEmpty()) View.VISIBLE else View.GONE
        deploymentImageRecycler.visibility =
            if (deploymentImages.isNotEmpty()) View.VISIBLE else View.GONE
        val items = deploymentImages.map { it.toDeploymentImageView() }
        deploymentImageAdapter.setImages(items)
    }

    private fun setupToolbar() {
        setSupportActionBar(toolbar)
        supportActionBar?.apply {
            setDisplayShowTitleEnabled(true)
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowHomeEnabled(true)
            title =
                if (isConnected != false) locationName else locationName
        }
    }

    private fun setupLocationData() {
        val latitude = lat
        val longitude = long
        if(latitude != null && longitude != null) {
            latitudeValue.text = latitude.latitudeCoordinates(this)
            longitudeValue.text = longitude.longitudeCoordinates(this)
            altitudeValue.text = altitude.toString()
        }
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
            })
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
        switchPrefs = this.resources.getStringArray(R.array.switch_prefs).toList()
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

    private fun setupEditLocationButton() {
        editLocationButton.setOnClickListener {
            editLocation()
        }
    }

    private fun editLocation() {
        deployment?.let {
            val location = deployment?.stream
            location?.let { locate ->
                val group = locate.project?.name ?: getString(R.string.none)
                val isGroupExisted = locationGroupDb.isExisted(locate.project?.name)
                analytics.trackEditLocationEvent()
                EditLocationActivity.startActivity(
                    this,
                    locate.latitude,
                    locate.longitude,
                    locate.altitude,
                    locate.name,
                    it.id,
                    if (isGroupExisted) group else getString(R.string.none),
                    Device.GUARDIAN.value,
                    DIAGNOSTIC_REQUEST_CODE
                )
            }
        }
    }

    private fun setupSyncButton() {
        syncButton.setOnClickListener {
            syncPrefs()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == DIAGNOSTIC_REQUEST_CODE) {
            forceUpdateDeployment()
        }
    }

    private fun forceUpdateDeployment() {
        if (this.deployment != null) {
            this.deployment = guardianDeploymentDb.getDeploymentById(this.deployment!!.id)
            this.deployment?.let { it1 ->
                updateDeploymentDetailView(it1)
            }

            supportActionBar?.apply {
                title = deployment?.stream?.name ?: getString(R.string.title_deployment_detail)
            }
        }
    }

    private fun updateDeploymentDetailView(deployment: GuardianDeployment) {
        val location = deployment.stream
        location?.let { locate ->
            latitudeValue.text = locate.latitude.latitudeCoordinates(this)
            longitudeValue.text = locate.longitude.longitudeCoordinates(this)
            altitudeValue.text = locate.altitude.toString()
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

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }

    override fun setPrefsChanges(prefs: Map<String, String>) {
        this.prefsChanges = prefs
    }

    override fun getPrefsChanges(): String { /* not used */ return "" }

    override fun showSyncButton() {
        syncButton.visibility = View.VISIBLE
    }

    override fun hideSyncButton() {
        syncButton.visibility = View.INVISIBLE
    }

    override fun syncPrefs() {
        if (this.prefsChanges!!.isNotEmpty()) {
            val json = JsonObject()
            this.prefsChanges?.forEach {
                json.addProperty(it.key, it.value)
            }

            SocketManager.syncConfiguration(json.toString())
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
            .setAction(R.string.retry) { syncPrefs() }
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

    override fun onResume() {
        super.onResume()
        analytics.trackScreen(Screen.GUARDIAN_DIAGNOSTIC)
    }

    companion object {
        private const val TAG_LOADING_DIALOG = "TAG_LOADING_DIALOG"
        private const val DIAGNOSTIC_REQUEST_CODE = 1005
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

    override fun invoke(deploymentImage: DeploymentImageView) {
        val list = deploymentImages.map {
            if (it.remotePath != null) BuildConfig.DEVICE_API_DOMAIN + it.remotePath else "file://${it.localPath}"
        } as ArrayList

        val index = list.indexOf(deploymentImage.remotePath ?: "file://${deploymentImage.localPath}")
        list.removeAt(index)
        list.add(0, deploymentImage.remotePath ?: "file://${deploymentImage.localPath}")

        DisplayImageActivity.startActivity(this, list)    }
}
