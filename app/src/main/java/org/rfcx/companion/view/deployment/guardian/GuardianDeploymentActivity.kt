package org.rfcx.companion.view.deployment.guardian

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.location.Location
import android.location.LocationManager
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import io.realm.Realm
import kotlinx.android.synthetic.main.activity_guardian_deployment.*
import kotlinx.android.synthetic.main.toolbar_default.*
import org.rfcx.companion.R
import org.rfcx.companion.connection.socket.SocketManager
import org.rfcx.companion.connection.wifi.WifiHotspotManager
import org.rfcx.companion.connection.wifi.WifiLostListener
import org.rfcx.companion.entity.*
import org.rfcx.companion.entity.guardian.GuardianConfiguration
import org.rfcx.companion.entity.guardian.GuardianDeployment
import org.rfcx.companion.localdb.*
import org.rfcx.companion.localdb.guardian.GuardianDeploymentDb
import org.rfcx.companion.service.DownloadStreamState
import org.rfcx.companion.service.DownloadStreamsWorker
import org.rfcx.companion.service.GuardianDeploymentSyncWorker
import org.rfcx.companion.util.Analytics
import org.rfcx.companion.util.Preferences
import org.rfcx.companion.util.RealmHelper
import org.rfcx.companion.util.geojson.GeoJsonUtils
import org.rfcx.companion.view.deployment.EdgeDeploymentActivity
import org.rfcx.companion.view.deployment.guardian.advanced.GuardianAdvancedFragment
import org.rfcx.companion.view.deployment.guardian.checkin.GuardianCheckInTestFragment
import org.rfcx.companion.view.deployment.guardian.configure.GuardianConfigureFragment
import org.rfcx.companion.view.deployment.guardian.connect.ConnectGuardianFragment
import org.rfcx.companion.view.deployment.guardian.deploy.GuardianDeployFragment
import org.rfcx.companion.view.deployment.guardian.microphone.GuardianMicrophoneFragment
import org.rfcx.companion.view.deployment.guardian.register.GuardianRegisterFragment
import org.rfcx.companion.view.deployment.guardian.signal.GuardianSignalFragment
import org.rfcx.companion.view.deployment.guardian.solarpanel.GuardianSolarPanelFragment
import org.rfcx.companion.view.deployment.locate.LocationFragment
import org.rfcx.companion.view.deployment.locate.MapPickerFragment
import org.rfcx.companion.view.deployment.locate.SelectingExistedSiteFragment
import org.rfcx.companion.view.detail.MapPickerProtocol
import org.rfcx.companion.view.dialog.*
import org.rfcx.companion.view.prefs.SyncPreferenceListener
import java.util.*

class GuardianDeploymentActivity : AppCompatActivity(), GuardianDeploymentProtocol,
    CompleteListener, MapPickerProtocol, SyncPreferenceListener {
    // manager database
    private val realm by lazy { Realm.getInstance(RealmHelper.migrationConfig()) }
    private val locateDb by lazy { LocateDb(realm) }
    private val locationGroupDb by lazy { LocationGroupDb(realm) }
    private val deploymentDb by lazy { GuardianDeploymentDb(realm) }
    private val edgeDeploymentDb by lazy { EdgeDeploymentDb(realm) }
    private val deploymentImageDb by lazy { DeploymentImageDb(realm) }
    private val trackingDb by lazy { TrackingDb(realm) }
    private val trackingFileDb by lazy { TrackingFileDb(realm) }

    private var _deployment: GuardianDeployment? = null
    private var _deployLocation: DeploymentLocation? = null
    private var _configuration: GuardianConfiguration? = null
    private var _images: List<String> = listOf()
    private var _locate: Locate? = null

    private var useExistedLocation: Boolean = false

    private var currentLocation: Location? = null

    private var _sampleRate = 24000

    private var latitude = 0.0
    private var longitude = 0.0
    private var altitude = 0.0

    private var lastCheckInTime: Long? = null

    private var prefsChanges = mapOf<String, String>()
    private var prefsEditor: SharedPreferences.Editor? = null

    private var currentCheck = 0
    private var currentCheckName = ""
    private var passedChecks = arrayListOf<Int>()

    private var onDeployClicked = false

    private lateinit var wifiHotspotManager: WifiHotspotManager
    private val analytics by lazy { Analytics(this) }

    private val preferences = Preferences.getInstance(this)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_guardian_deployment)

        setupToolbar()

        val deploymentId = intent.extras?.getInt(EXTRA_DEPLOYMENT_ID)
        if (deploymentId != null) {
            val deployment = deploymentDb.getDeploymentById(deploymentId)
            if (deployment != null) {
                setDeployment(deployment)

                if (deployment.stream != null) {
                    _deployLocation = deployment.stream
                }

                if (deployment.configuration != null) {
                    _configuration = deployment.configuration
                }
            }
        } else {
            setupView()
        }
    }

    private fun setupToolbar() {
        setSupportActionBar(toolbar)
        supportActionBar?.apply {
            setDisplayShowTitleEnabled(true)
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowHomeEnabled(true)
        }
    }

    private fun setupView() {
        startFragment(ConnectGuardianFragment.newInstance())
    }

    override fun nextStep() {
        if (currentCheck !in passedChecks) {
            passedChecks.add(currentCheck)
        }
        startCheckList()
    }

    override fun backStep() {
        val container = supportFragmentManager.findFragmentById(R.id.contentContainer)
        when (container) {
            is MapPickerFragment -> startFragment(LocationFragment.newInstance())
            is GuardianCheckListFragment -> {
                SocketManager.resetAllValuesToDefault()
                setLastCheckInTime(null)
                SocketManager.getCheckInTest() // to stop getting checkin test
                passedChecks.clear() // remove all passed
                unregisterWifiConnectionLostListener()
                startFragment(ConnectGuardianFragment.newInstance())
            }
            is ConnectGuardianFragment -> finish()
            else -> startCheckList()
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        backStep()
        return true
    }

    override fun startCheckList() {
        startFragment(GuardianCheckListFragment.newInstance())
    }

    override fun startSelectingExistedSite(latitude: Double, longitude: Double) {
        startFragment(SelectingExistedSiteFragment.newInstance(latitude, longitude))
    }

    override fun isOpenedFromUnfinishedDeployment(): Boolean = false // guardian not have this feature so return false

    override fun getDeployment(): GuardianDeployment? = this._deployment

    override fun setDeployment(deployment: GuardianDeployment) {
        this._deployment = deployment
    }

    override fun setDeploymentWifiName(name: String) {
        val deployment = _deployment ?: GuardianDeployment()
        deployment.wifiName = name
        setDeployment(deployment)
    }

    override fun setSampleRate(sampleRate: Int) {
        this._sampleRate = sampleRate
    }

    override fun setOnDeployClicked() {
        this.onDeployClicked = true
    }

    override fun setLastCheckInTime(time: Long?) {
        this.lastCheckInTime = time
    }

    override fun addRegisteredToPassedCheck() {
        if (1 !in passedChecks) {
            passedChecks.add(1)
        }
    }

    override fun removeRegisteredOnPassedCheck() {
        if (1 in passedChecks) {
            passedChecks.remove(1)
        }
    }

    override fun setImages(images: List<String>) {
        this._images = images
    }

    override fun setCurrentPage(name: String) {
        currentCheckName = name
    }

    override fun setDeploymentConfigure(config: GuardianConfiguration) {
        this._configuration = config
        this._deployment?.configuration = _configuration

        // update deployment
        this._deployment?.let { deploymentDb.updateDeployment(it) }
    }

    override fun getConfiguration(): GuardianConfiguration? = _configuration

    override fun getSampleRate(): Int = _sampleRate

    override fun getWifiName(): String = _deployment?.wifiName ?: ""

    override fun getLastCheckInTime(): Long? = lastCheckInTime

    override fun getDeploymentLocation(): DeploymentLocation? = this._deployLocation

    override fun getLocationGroup(name: String): LocationGroups? {
        return locationGroupDb.getLocationGroup(name)
    }

    override fun getImages(): List<String> {
        return this._images
    }

    override fun getCurrentLocation(): Location = currentLocation ?: Location(LocationManager.GPS_PROVIDER)

    override fun setDeployLocation(locate: Locate, isExisted: Boolean) {
        val deployment = _deployment ?: GuardianDeployment()
        deployment.isActive = locate.serverId == null
        deployment.state = DeploymentState.Guardian.Locate.key // state

        this._deployLocation = locate.asDeploymentLocation()
        val deploymentId = deploymentDb.insertOrUpdateDeployment(deployment, _deployLocation!!)
        deployment.id = deploymentId

        useExistedLocation = isExisted
        this._locate = locate
        if (!useExistedLocation) {
            locateDb.insertOrUpdateLocate(deploymentId, locate, true) // update locate - last deployment
        }

        setDeployment(deployment)
    }

    override fun setReadyToDeploy() {
        showLoading()
        this._deployment?.let {
            it.deployedAt = Date()
            it.updatedAt = Date()
            it.isActive = true
            it.state = DeploymentState.Guardian.ReadyToUpload.key
            setDeployment(it)

            if (useExistedLocation) {
                this._locate?.let { locate ->
                    locateDb.insertOrUpdateLocate(it.id, locate, true) // update locate - last deployment
                    val deployments = locate.serverId?.let { it1 -> deploymentDb.getDeploymentsBySiteId(it1) }
                    val edgeDeployments = locate.serverId?.let { it1 -> edgeDeploymentDb.getDeploymentsBySiteId(it1) }
                    deployments?.forEach { deployment ->
                        deploymentDb.updateIsActive(deployment.id)
                    }
                    edgeDeployments?.forEach { deployment ->
                        edgeDeploymentDb.updateIsActive(deployment.id)
                    }
                }
            }

            saveImages(it)
            deploymentDb.updateDeployment(it)

            //track getting
            if (preferences.getBoolean(Preferences.ENABLE_LOCATION_TRACKING)) {
                val track = trackingDb.getFirstTracking()
                track?.let { t ->
                    val point = t.points.toListDoubleArray()
                    val trackingFile = TrackingFile(
                        deploymentId = it.id,
                        siteId = this._locate!!.id,
                        localPath = GeoJsonUtils.generateGeoJson(this, GeoJsonUtils.generateFileName(it.deployedAt, it.wifiName!!), point).absolutePath
                    )
                    trackingFileDb.insertOrUpdate(trackingFile)
                }
            }

            analytics.trackCreateGuardianDeploymentEvent()

            SocketManager.getCheckInTest() // to stop getting checkin test
            GuardianDeploymentSyncWorker.enqueue(this@GuardianDeploymentActivity)
            showComplete()
        }
    }

    private fun saveImages(deployment: GuardianDeployment) {
        deploymentImageDb.deleteImages(deployment.id)
        deploymentImageDb.insertImage(null, deployment, _images)
    }

    override fun setCurrentLocation(location: Location) {
        this.currentLocation = location
    }

    override fun startSetupConfigure() {
        startFragment(GuardianConfigureFragment.newInstance())
    }

    override fun backToConfigure() {
        startFragment(GuardianConfigureFragment.newInstance())
    }

    override fun handleCheckClicked(number: Int) {
        // setup fragment for current step
        currentCheck = number
        when (number) {
            0 -> {
                updateDeploymentState(DeploymentState.Guardian.SolarPanel)
                startFragment(GuardianSolarPanelFragment.newInstance())
            }
            1 -> {
                updateDeploymentState(DeploymentState.Guardian.Register)
                startFragment(GuardianRegisterFragment.newInstance())
            }
            2 -> {
                updateDeploymentState(DeploymentState.Guardian.Signal)
                startFragment(GuardianSignalFragment.newInstance())
            }
            3 -> {
                updateDeploymentState(DeploymentState.Guardian.Microphone)
                startFragment(GuardianMicrophoneFragment.newInstance())
            }
            4 -> {
                updateDeploymentState(DeploymentState.Guardian.Config)
                startFragment(GuardianConfigureFragment.newInstance())
            }
            5 -> {
                updateDeploymentState(DeploymentState.Guardian.Locate)
                startFragment(LocationFragment.newInstance())
            }
            6 -> {
                updateDeploymentState(DeploymentState.Guardian.Checkin)
                startFragment(GuardianCheckInTestFragment.newInstance())
            }
            7 -> {
                updateDeploymentState(DeploymentState.Guardian.Deploy)
                startFragment(GuardianDeployFragment.newInstance())
            }
            8 -> {
                updateDeploymentState(DeploymentState.Guardian.Advanced)
                startFragment(GuardianAdvancedFragment.newInstance())
            }
        }
    }

    override fun getPassedChecks(): List<Int> = passedChecks

    private fun startFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(contentContainer.id, fragment)
            .commit()
    }

    private fun updateDeploymentState(state: DeploymentState.Guardian) {
        this._deployment?.state = state.key
        this._deployment?.let { deploymentDb.updateDeployment(it) }
    }

    override fun showConnectInstruction() {
        val instructionDialog: ConnectInstructionDialogFragment =
            supportFragmentManager.findFragmentByTag(TAG_INSTRUCTION_DIALOG) as ConnectInstructionDialogFragment?
                ?: run {
                    ConnectInstructionDialogFragment()
                }
        instructionDialog.show(supportFragmentManager, TAG_INSTRUCTION_DIALOG)
    }

    override fun showSiteLoadingDialog(text: String) {
        var siteLoadingDialog: SiteLoadingDialogFragment =
            supportFragmentManager.findFragmentByTag(EdgeDeploymentActivity.TAG_SITE_LOADING_DIALOG) as SiteLoadingDialogFragment?
                ?: run {
                    SiteLoadingDialogFragment(text)
                }
        if (siteLoadingDialog.isAdded) {
            siteLoadingDialog.dismiss()
            siteLoadingDialog = SiteLoadingDialogFragment(text)
        }
        siteLoadingDialog.show(supportFragmentManager,
            EdgeDeploymentActivity.TAG_SITE_LOADING_DIALOG
        )
    }

    override fun showLoading() {
        val loadingDialog: LoadingDialogFragment =
            supportFragmentManager.findFragmentByTag(TAG_LOADING_DIALOG) as LoadingDialogFragment?
                ?: run {
                    LoadingDialogFragment()
                }
        loadingDialog.show(supportFragmentManager, TAG_LOADING_DIALOG)
    }

    private fun showComplete() {
        val completeFragment: CompleteFragment =
            supportFragmentManager.findFragmentByTag(CompleteFragment.tag) as CompleteFragment?
                ?: run {
                    CompleteFragment()
                }
        completeFragment.show(supportFragmentManager, CompleteFragment.tag)
    }

    override fun hideLoading() {
        val loadingDialog: LoadingDialogFragment? =
            supportFragmentManager.findFragmentByTag(TAG_LOADING_DIALOG) as LoadingDialogFragment?
        loadingDialog?.dismissDialog()
    }

    override fun setWifiManager(wifiManager: WifiHotspotManager) {
        wifiHotspotManager = wifiManager
    }

    override fun registerWifiConnectionLostListener() {
        wifiHotspotManager.registerWifiConnectionLost(object : WifiLostListener {
            override fun onLost() {
                if (!onDeployClicked) {
                    val wifiLostDialog: WifiLostDialogFragment =
                        supportFragmentManager.findFragmentByTag(TAG_WIFI_LOST_DIALOG) as WifiLostDialogFragment?
                            ?: run {
                                WifiLostDialogFragment()
                            }
                    wifiLostDialog.show(supportFragmentManager, TAG_WIFI_LOST_DIALOG)
                }
            }
        })
    }

    override fun unregisterWifiConnectionLostListener() {
        if (::wifiHotspotManager.isInitialized) {
            wifiHotspotManager.unregisterWifiConnectionLost()
        }
    }

    override fun showToolbar() {
        toolbar.visibility = View.VISIBLE
    }

    override fun hideToolbar() {
        toolbar.visibility = View.GONE
    }

    override fun setToolbarTitle() {
        supportActionBar?.apply {
            title = currentCheckName
        }
    }

    override fun isSiteLoading(): DownloadStreamState {
        return DownloadStreamsWorker.isRunning()
    }

    override fun startMapPicker(latitude: Double, longitude: Double, altitude: Double, name: String) {
        setLatLng(latitude, longitude, altitude)
        startFragment(MapPickerFragment.newInstance(latitude, longitude, altitude, name))
    }

    private fun setLatLng(latitude: Double, longitude: Double, altitude: Double) {
        this.latitude = latitude
        this.longitude = longitude
        this.altitude = altitude
    }

    override fun startLocationPage(
        latitude: Double,
        longitude: Double,
        altitude: Double,
        name: String,
        fromPicker: Boolean
    ) {
        startFragment(LocationFragment.newInstance(latitude, longitude, altitude, name, fromPicker))
    }

    override fun onBackPressed() {
        backStep()
    }

    override fun onAnimationEnd() {
        finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        this.prefsEditor?.clear()?.apply()
        unregisterWifiConnectionLostListener()
    }

    companion object {
        private const val TAG_WIFI_LOST_DIALOG = "TAG_WIFI_LOST_DIALOG"
        private const val TAG_INSTRUCTION_DIALOG = "TAG_INSTRUCTION_DIALOG"
        private const val TAG_LOADING_DIALOG = "TAG_LOADING_DIALOG"
        private const val EXTRA_DEPLOYMENT_ID = "EXTRA_DEPLOYMENT_ID"

        fun startActivity(context: Context) {
            val intent = Intent(context, GuardianDeploymentActivity::class.java)
            context.startActivity(intent)
        }

        fun startActivity(context: Context, deploymentId: Int) {
            val intent = Intent(context, GuardianDeploymentActivity::class.java)
            intent.putExtra(EXTRA_DEPLOYMENT_ID, deploymentId)
            context.startActivity(intent)
        }
    }

    override fun setPrefsChanges(prefs: Map<String, String>) {
        this.prefsChanges = prefs
    }

    override fun getPrefsChanges(): List<String> {
        val listForGuardian = mutableListOf<String>()
        if (this.prefsChanges.isNotEmpty()) {
            this.prefsChanges.forEach {
                listForGuardian.add("${it.key}|${it.value}")
            }
        }
        return listForGuardian
    }

    override fun showSyncButton() { /* not used */
    }

    override fun hideSyncButton() { /* not used */
    }

    override fun syncPrefs() { /* not used */
    }

    override fun showSuccessResponse() { /* not used */
    }

    override fun showFailedResponse() { /* not used */
    }

    override fun setEditor(editor: SharedPreferences.Editor) {
        this.prefsEditor = editor
    }
}
