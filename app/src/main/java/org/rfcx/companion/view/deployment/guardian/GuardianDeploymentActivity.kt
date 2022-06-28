package org.rfcx.companion.view.deployment.guardian

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.location.Location
import android.location.LocationManager
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.Transformations
import androidx.preference.Preference
import com.google.android.material.snackbar.Snackbar
import com.google.gson.Gson
import com.google.gson.JsonObject
import io.realm.Realm
import kotlinx.android.synthetic.main.activity_guardian_deployment.*
import kotlinx.android.synthetic.main.toolbar_default.*
import org.rfcx.companion.R
import org.rfcx.companion.connection.socket.AdminSocketManager
import org.rfcx.companion.connection.socket.GuardianSocketManager
import org.rfcx.companion.connection.wifi.WifiHotspotManager
import org.rfcx.companion.connection.wifi.WifiLostListener
import org.rfcx.companion.entity.*
import org.rfcx.companion.entity.guardian.ClassifierPing
import org.rfcx.companion.entity.guardian.Deployment
import org.rfcx.companion.entity.socket.response.GuardianPing
import org.rfcx.companion.entity.socket.response.I2CAccessibility
import org.rfcx.companion.entity.socket.response.SentinelInfo
import org.rfcx.companion.entity.socket.response.SpeedTest
import org.rfcx.companion.localdb.*
import org.rfcx.companion.service.DeploymentSyncWorker
import org.rfcx.companion.util.*
import org.rfcx.companion.util.geojson.GeoJsonUtils
import org.rfcx.companion.util.prefs.GuardianPlan
import org.rfcx.companion.util.socket.PingUtils
import org.rfcx.companion.view.deployment.BaseDeploymentActivity
import org.rfcx.companion.view.deployment.guardian.advanced.GuardianAdvancedFragment
import org.rfcx.companion.view.deployment.guardian.checkin.GuardianCheckInTestFragment
import org.rfcx.companion.view.deployment.guardian.classifier.ClassifierLoadFragment
import org.rfcx.companion.view.deployment.guardian.communication.GuardianCommunicationFragment
import org.rfcx.companion.view.deployment.guardian.configure.GuardianConfigureFragment
import org.rfcx.companion.view.deployment.guardian.connect.ConnectGuardianFragment
import org.rfcx.companion.view.deployment.guardian.deploy.GuardianDeployFragment
import org.rfcx.companion.view.deployment.guardian.microphone.GuardianMicrophoneFragment
import org.rfcx.companion.view.deployment.guardian.register.GuardianRegisterFragment
import org.rfcx.companion.view.deployment.guardian.signal.GuardianSignalFragment
import org.rfcx.companion.view.deployment.guardian.softwareupdate.SoftwareUpdateFragment
import org.rfcx.companion.view.deployment.guardian.solarpanel.GuardianSolarPanelFragment
import org.rfcx.companion.view.deployment.locate.MapPickerFragment
import org.rfcx.companion.view.deployment.locate.SiteWithLastDeploymentItem
import org.rfcx.companion.view.deployment.location.DetailDeploymentSiteFragment
import org.rfcx.companion.view.deployment.location.SetDeploymentSiteFragment
import org.rfcx.companion.view.dialog.CompleteFragment
import org.rfcx.companion.view.dialog.ConnectInstructionDialogFragment
import org.rfcx.companion.view.dialog.LoadingDialogFragment
import org.rfcx.companion.view.prefs.SyncPreferenceListener
import java.util.*

class GuardianDeploymentActivity :
    BaseDeploymentActivity(),
    GuardianDeploymentProtocol,
    SyncPreferenceListener,
    WifiLostListener {
    // manager database
    private val realm by lazy { Realm.getInstance(RealmHelper.migrationConfig()) }
    private val streamDb by lazy { StreamDb(realm) }
    private val projectDb by lazy { ProjectDb(realm) }
    private val deploymentDb by lazy { DeploymentDb(realm) }
    private val deploymentImageDb by lazy { DeploymentImageDb(realm) }
    private val trackingDb by lazy { TrackingDb(realm) }
    private val trackingFileDb by lazy { TrackingFileDb(realm) }

    private var useExistedLocation: Boolean = false

    private var guardianPingBlob: GuardianPing? = null
    private var network: Int? = null
    private var swmNetwork: Int? = null
    private var swmUnsentMsgs: Int? = null
    private var sentinelPower: SentinelInfo? = null
    private var internalBattery: Int? = null
    private var i2cAccessibility: I2CAccessibility? = null
    private var isGuardianRegistered: Boolean? = null
    private var isSimDetected: Boolean? = null
    private var satId: String? = null
    private var isGPSDetected: Boolean? = null
    private var phoneNumber: String? = null
    private var guardianPlan: GuardianPlan? = null
    private var satTimeOff: List<String>? = null
    private var speedTest: SpeedTest? = null
    private var guardianLocalTime: Long? = null
    private var guardianTimezone: String? = null
    private var classifiers: Map<String, ClassifierPing>? = null
    private var activeClassifiers: Map<String, ClassifierPing>? = null

    private var _sampleRate = 12000

    private var lastCheckInTime: Long? = null

    private var prefsChanges = mapOf<String, String>()
    private var prefsEditor: SharedPreferences.Editor? = null

    private var currentCheck = -1
    private var passedChecks = arrayListOf<Int>()

    private var onDeployClicked = false
    private var menuAll: Menu? = null

    private val analytics by lazy { Analytics(this) }

    private val preferences = Preferences.getInstance(this)

    // Local LiveData
    private lateinit var deploymentLiveData: LiveData<List<Deployment>>
    private var guardianDeployments = listOf<Deployment>()
    private val guardianDeploymentObserve = Observer<List<Deployment>> {
        this.guardianDeployments = it.filter { deployment -> deployment.isCompleted() }
        setSiteItems()
    }

    private lateinit var siteLiveData: LiveData<List<Stream>>
    private var sites = listOf<Stream>()
    private val siteObserve = Observer<List<Stream>> {
        this.sites = it
        setSiteItems()
    }

    private val wifiHotspotManager by lazy { WifiHotspotManager(this) }
    private var timer: Timer? = null

    @SuppressLint("ResourceAsColor")
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuAll = menu
        val inflater = menuInflater
        inflater.inflate(R.menu.preference_menu, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> backStep()
            R.id.MoreView -> onClickMoreView()
        }
        return super.onOptionsItemSelected(item)
    }

    private fun onClickMoreView() {
        setCurrentPage(getString(R.string.advanced_config))
        setToolbarTitle()
        startFragment(GuardianAdvancedFragment.newInstance())
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_guardian_deployment)

        setupToolbar()
        setLiveData()

        this.currentLocate = this.getLastLocation()

        val deploymentId = intent.extras?.getInt(EXTRA_DEPLOYMENT_ID)
        if (deploymentId != null) {
            val deployment = deploymentDb.getDeploymentById(deploymentId)
            if (deployment != null) {
                setDeployment(deployment)
                if (deployment.stream != null) {
                    _stream = deployment.stream
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
        val container = supportFragmentManager.findFragmentById(R.id.contentContainer)
        if (container !is GuardianAdvancedFragment) {
            if (currentCheck !in passedChecks) {
                passedChecks.add(currentCheck)
            }
            currentCheck = -1 // reset check
        }
        startCheckList()
    }

    override fun backStep() {
        currentCheck = -1 // reset check
        val container = supportFragmentManager.findFragmentById(R.id.contentContainer)
        when (container) {
            is GuardianAdvancedFragment -> {
                startCheckList()
            }
            is MapPickerFragment -> {
                startFragment(
                    DetailDeploymentSiteFragment.newInstance(
                        latitude,
                        longitude,
                        siteId
                    )
                )
            }
            is GuardianCheckListFragment -> {
                setLastCheckInTime(null)
                SocketUtils.stopAllConnections()
                SocketUtils.clearAllBlobs()
                stopPeriodicSocketHeartBreath()
                passedChecks.clear() // remove all passed
                startFragment(ConnectGuardianFragment.newInstance())
            }
            is ConnectGuardianFragment -> finish()
            else -> {
                startCheckList()
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        backStep()
        return true
    }

    private fun setLiveData() {
        val projectId = preferences.getInt(Preferences.SELECTED_PROJECT)
        val project = projectDb.getProjectById(projectId)
        val projectName = project?.name ?: getString(R.string.none)
        siteLiveData = Transformations.map(
            streamDb.getAllResultsAsyncWithinProject(id = projectId).asLiveData()
        ) {
            it
        }
        siteLiveData.observeForever(siteObserve)

        deploymentLiveData =
            Transformations.map(
                deploymentDb.getAllResultsAsyncWithinProject(id = projectId).asLiveData()
            ) {
                it
            }

        GuardianSocketManager.pingBlob.observeForever {
            guardianPingBlob = it
            isGuardianRegistered = PingUtils.isRegisteredFromPing(it)
            if (isGuardianRegistered == true) {
                addRegisteredToPassedCheck()
            }
            swmNetwork = PingUtils.getSwarmNetworkFromPing(it)
            swmUnsentMsgs = PingUtils.getSwarmUnsetMessagesFromPing(it)
            internalBattery = PingUtils.getInternalBatteryFromPing(it)
            guardianPlan = PingUtils.getGuardianPlanFromPrefs(it)
            satTimeOff = PingUtils.getSatTimeOffFromPrefs(it)
            guardianLocalTime = PingUtils.getGuardianLocalTime(it)
            guardianTimezone = PingUtils.getGuardianTimezone(it)
            _sampleRate = PingUtils.getSampleRateFromPrefs(it) ?: 12000
            classifiers = PingUtils.getClassifiers(it)
            activeClassifiers = PingUtils.getActiveClassifiers(it)
        }
        AdminSocketManager.pingBlob.observeForever {
            network = PingUtils.getNetworkFromPing(it)
            sentinelPower = PingUtils.getSentinelPowerFromPing(it)
            i2cAccessibility = PingUtils.getI2cAccessibilityFromPing(it)
            satId = PingUtils.getSwarmIdFromPing(it)
            isSimDetected = PingUtils.getSimDetectedFromPing(it)
            isGPSDetected = PingUtils.getGPSDetectedFromPing(it)
            phoneNumber = PingUtils.getPhoneNumberFromPing(it)
            speedTest = PingUtils.getSpeedTest(it)
        }
        deploymentLiveData.observeForever(guardianDeploymentObserve)
    }

    override fun onLost() {
        unregisterWifiLost()
        Snackbar.make(guardianRootView, getString(R.string.guardian_lost_wifi), Snackbar.LENGTH_LONG)
            .setAction(
                getString(R.string.guardian_go_back_wifi)
            ) {
                startConnectGuardian()
            }
            .show()
    }

    private fun setSiteItems() {
        val loc = Location(LocationManager.GPS_PROVIDER)
        loc.latitude = 0.0
        loc.longitude = 0.0

        _siteItems = getListSite(
            currentLocate ?: loc,
            sites
        )
    }

    override fun startCheckList() {
        registerWifiLost()
        periodicSocketHeartBreath()
        startFragment(GuardianCheckListFragment.newInstance())
    }

    override fun isOpenedFromUnfinishedDeployment(): Boolean =
        false // guardian not have this feature so return false

    override fun getDeployment(): Deployment? = this._deployment ?: Deployment()

    override fun setDeployment(deployment: Deployment) {
        this._deployment = deployment
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
        if (3 !in passedChecks) {
            passedChecks.add(4)
        }
    }

    override fun removeRegisteredOnPassedCheck() {
        if (1 in passedChecks) {
            passedChecks.remove(1)
        }
    }

    override fun getSampleRate(): Int = _sampleRate

    override fun getLastCheckInTime(): Long? = lastCheckInTime

    override fun getNetwork(): Int? = network

    override fun getSwmNetwork(): Int? = swmNetwork

    override fun getSwmUnsentMessages(): Int? = swmUnsentMsgs

    override fun getSentinelPower(): SentinelInfo? = sentinelPower

    override fun getInternalBattery(): Int? = internalBattery

    override fun getI2cAccessibility(): I2CAccessibility? = i2cAccessibility

    override fun getSimDetected(): Boolean? = isSimDetected

    override fun getSatId(): String? = satId

    override fun getGPSDetected(): Boolean? = isGPSDetected

    override fun getPhoneNumber(): String? = phoneNumber

    override fun getGuardianPlan(): GuardianPlan? = guardianPlan

    override fun getSatTimeOff(): List<String>? = satTimeOff

    override fun getSpeedTest(): SpeedTest? = speedTest

    override fun getGuardianLocalTime(): Long? = guardianLocalTime

    override fun getGuardianTimezone(): String? = guardianTimezone

    override fun getClassifiers(): Map<String, ClassifierPing>? = classifiers

    override fun getActiveClassifiers(): Map<String, ClassifierPing>? = activeClassifiers

    override fun getCurrentProjectId(): String? {
        val projectId = preferences.getInt(Preferences.SELECTED_PROJECT)
        val project = projectDb.getProjectById(projectId)
        return project?.serverId
    }

    override fun getGuid(): String? = PingUtils.getGuidFromPing(guardianPingBlob)

    override fun getGuardianPurpose(): String? = PingUtils.getPurposeFromPrefs(guardianPingBlob)

    override fun isGuardianRegistered(): Boolean? = isGuardianRegistered

    override fun getSoftwareVersion(): Map<String, String>? = PingUtils.getSoftwareVersionFromPing(guardianPingBlob)

    override fun getAudioConfiguration(): JsonObject? = PingUtils.getAudioConfigureFromPing(guardianPingBlob)

    override fun getPrefsSha1(): String? = PingUtils.getPrefsSha1FromPing(guardianPingBlob)

    override fun getLatestCheckIn(): JsonObject? = PingUtils.getLatestCheckInFromPing(guardianPingBlob)

    override fun reTriggerConnection() {
        GuardianSocketManager.getConnection()
        AdminSocketManager.connect()
    }

    override fun startPeriodicHeartbeat() {
        periodicSocketHeartBreath()
    }

    override fun stopPeriodicHeartbeat() {
        stopPeriodicSocketHeartBreath()
    }

    override fun getDeploymentStream(): Stream? = this._stream

    override fun getSiteItem(): List<SiteWithLastDeploymentItem> = this._siteItems

    override fun getStream(id: Int): Stream? {
        return streamDb.getStreamById(id)
    }

    override fun getProject(id: Int): Project? {
        return projectDb.getProjectById(id)
    }

    override fun setDeployLocation(stream: Stream, isExisted: Boolean) {
        val deployment = _deployment ?: Deployment()
        deployment.isActive = stream.serverId == null
        deployment.state = DeploymentState.Guardian.Locate.key // state

        this._stream = stream
        useExistedLocation = isExisted

        setDeployment(deployment)
    }

    override fun setReadyToDeploy() {
        showLoading()
        this._deployment?.let {
            it.deployedAt = Date()
            it.updatedAt = Date()
            it.isActive = true
            it.state = DeploymentState.Guardian.ReadyToUpload.key
            it.deviceParameters = Gson().toJson(DeviceParameter(getGuid()))
            setDeployment(it)

            // set all deployments in stream to active false
            if (useExistedLocation) {
                this._stream?.let { locate ->
                    locate.deployments?.forEach { dp ->
                        deploymentDb.updateIsActive(dp.id)
                    }
                }
            }

            this._stream?.let { loc ->
                val streamId = streamDb.insertOrUpdate(loc)
                val deploymentId = deploymentDb.insertOrUpdateDeployment(it, streamId)
                streamDb.updateDeploymentIdOnStream(deploymentId, streamId) // update locate - last deployment
            }
            saveImages(it)

            // track getting
            if (preferences.getBoolean(Preferences.ENABLE_LOCATION_TRACKING)) {
                val track = trackingDb.getFirstTracking()
                track?.let { t ->
                    val point = t.points.toListDoubleArray()
                    val trackingFile = TrackingFile(
                        deploymentId = it.id,
                        siteId = this._stream!!.id,
                        localPath = GeoJsonUtils.generateGeoJson(
                            this,
                            GeoJsonUtils.generateFileName(it.deployedAt, getGuid()!!),
                            point
                        ).absolutePath
                    )
                    trackingFileDb.insertOrUpdate(trackingFile)
                }
            }

            analytics.trackCreateGuardianDeploymentEvent()

            DeploymentSyncWorker.enqueue(this@GuardianDeploymentActivity)
            showComplete()
        }
    }

    private fun saveImages(deployment: Deployment) {
        deploymentImageDb.deleteImages(deployment.id)
        deploymentImageDb.insertImage(deployment, _images)
    }

    override fun startConnectGuardian() {
        startFragment(ConnectGuardianFragment.newInstance())
    }

    override fun startSetupConfigure() {
        startFragment(GuardianConfigureFragment.newInstance())
    }

    override fun startGuardianRegister() {
        startFragment(GuardianRegisterFragment.newInstance())
    }

    override fun backToConfigure() {
        startFragment(GuardianConfigureFragment.newInstance())
    }

    override fun registerWifiLost() {
        wifiHotspotManager.registerWifiConnectionLost(this)
    }

    override fun unregisterWifiLost() {
        wifiHotspotManager.unRegisterWifiLost()
    }

    override fun handleCheckClicked(number: Int) {
        // setup fragment for current step
        currentCheck = number
        when (number) {
            0 -> {
                startFragment(SoftwareUpdateFragment.newInstance())
            }
            1 -> {
                startFragment(ClassifierLoadFragment.newInstance())
            }
            2 -> {
                startFragment(GuardianSolarPanelFragment.newInstance())
            }
            3 -> {
                startFragment(GuardianCommunicationFragment.newInstance())
            }
            4 -> {
                updateDeploymentState(DeploymentState.Guardian.Register)
                startFragment(GuardianRegisterFragment.newInstance())
            }
            5 -> {
                updateDeploymentState(DeploymentState.Guardian.Signal)
                startFragment(GuardianSignalFragment.newInstance())
            }
            6 -> {
                startFragment(GuardianConfigureFragment.newInstance())
            }
            7 -> {
                updateDeploymentState(DeploymentState.Guardian.Microphone)
                startFragment(GuardianMicrophoneFragment.newInstance())
            }
            8 -> {
                updateDeploymentState(DeploymentState.Guardian.Locate)
                val site = this._stream
                if (site == null) {
                    startFragment(
                        SetDeploymentSiteFragment.newInstance(
                            currentLocate?.latitude ?: 0.0, currentLocate?.longitude ?: 0.0
                        )
                    )
                } else {
                    startDetailDeploymentSite(site.latitude, site.longitude, site.id, site.name)
                }
            }
            9 -> {
                updateDeploymentState(DeploymentState.Guardian.Deploy)
                startFragment(GuardianDeployFragment.newInstance())
            }
            10 -> {
                updateDeploymentState(DeploymentState.Guardian.Checkin)
                startFragment(GuardianCheckInTestFragment.newInstance())
            }
        }
    }

    override fun getPassedChecks(): List<Int> = passedChecks

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

    override fun setMenuToolbar(isVisibility: Boolean) {
        menuAll?.findItem(R.id.MoreView)?.isVisible = isVisibility
    }

    override fun setToolbarSubtitle(sub: String) {
        supportActionBar?.apply {
            subtitle = sub
        }
    }

    override fun onBackPressed() {
        backStep()
    }

    override fun onDestroy() {
        super.onDestroy()
        this.prefsEditor?.clear()?.apply()
        siteLiveData.removeObserver(siteObserve)
        deploymentLiveData.removeObserver(guardianDeploymentObserve)
        SocketUtils.stopAllConnections()
        stopPeriodicSocketHeartBreath()
    }

    private fun periodicSocketHeartBreath() {
        if (timer != null) return
        timer = Timer()
        timer!!.schedule(
            object : TimerTask() {
                override fun run() {
                    reTriggerConnection()
                }
            },
            0, 10000
        )
    }

    private fun stopPeriodicSocketHeartBreath() {
        if (timer == null) return
        timer!!.cancel()
        timer!!.purge()
        timer = null
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

    override fun getPrefsChanges(): JsonObject {
        val json = JsonObject()
        if (this.prefsChanges.isNotEmpty()) {
            this.prefsChanges.forEach {
                json.addProperty(it.key, it.value)
            }
        }
        return json
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

    override fun getPrefs(): List<Preference> = PingUtils.getPrefsFromPing(this, guardianPingBlob)
}
