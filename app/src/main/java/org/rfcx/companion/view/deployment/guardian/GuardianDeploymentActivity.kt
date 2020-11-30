package org.rfcx.companion.view.deployment.guardian

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import io.realm.Realm
import java.util.*
import kotlinx.android.synthetic.main.activity_guardian_deployment.*
import kotlinx.android.synthetic.main.toolbar_default.*
import org.rfcx.companion.R
import org.rfcx.companion.connection.socket.SocketManager
import org.rfcx.companion.connection.wifi.WifiHotspotManager
import org.rfcx.companion.connection.wifi.WifiLostListener
import org.rfcx.companion.entity.DeploymentLocation
import org.rfcx.companion.entity.DeploymentState
import org.rfcx.companion.entity.Locate
import org.rfcx.companion.entity.LocationGroups
import org.rfcx.companion.entity.guardian.GuardianConfiguration
import org.rfcx.companion.entity.guardian.GuardianDeployment
import org.rfcx.companion.entity.guardian.GuardianProfile
import org.rfcx.companion.entity.socket.response.Signal
import org.rfcx.companion.entity.socket.response.SignalResponse
import org.rfcx.companion.localdb.LocateDb
import org.rfcx.companion.localdb.LocationGroupDb
import org.rfcx.companion.localdb.guardian.GuardianDeploymentDb
import org.rfcx.companion.localdb.guardian.GuardianDeploymentImageDb
import org.rfcx.companion.localdb.guardian.GuardianProfileDb
import org.rfcx.companion.service.GuardianDeploymentSyncWorker
import org.rfcx.companion.util.Analytics
import org.rfcx.companion.util.RealmHelper
import org.rfcx.companion.view.deployment.guardian.advanced.GuardianAdvancedFragment
import org.rfcx.companion.view.deployment.guardian.checkin.GuardianCheckInTestFragment
import org.rfcx.companion.view.deployment.guardian.configure.GuardianConfigureFragment
import org.rfcx.companion.view.deployment.guardian.configure.GuardianSelectProfileFragment
import org.rfcx.companion.view.deployment.guardian.connect.ConnectGuardianFragment
import org.rfcx.companion.view.deployment.guardian.deploy.GuardianDeployFragment
import org.rfcx.companion.view.deployment.guardian.microphone.GuardianMicrophoneFragment
import org.rfcx.companion.view.deployment.guardian.register.GuardianRegisterFragment
import org.rfcx.companion.view.deployment.guardian.signal.GuardianSignalFragment
import org.rfcx.companion.view.deployment.guardian.solarpanel.GuardianSolarPanelFragment
import org.rfcx.companion.view.deployment.locate.LocationFragment
import org.rfcx.companion.view.deployment.locate.MapPickerFragment
import org.rfcx.companion.view.detail.MapPickerProtocol
import org.rfcx.companion.view.dialog.*
import org.rfcx.companion.view.prefs.SyncPreferenceListener

class GuardianDeploymentActivity : AppCompatActivity(), GuardianDeploymentProtocol,
    CompleteListener, MapPickerProtocol, SyncPreferenceListener {
    // manager database
    private val realm by lazy { Realm.getInstance(RealmHelper.migrationConfig()) }
    private val locateDb by lazy { LocateDb(realm) }
    private val locationGroupDb by lazy { LocationGroupDb(realm) }
    private val profileDb by lazy { GuardianProfileDb(realm) }
    private val deploymentDb by lazy { GuardianDeploymentDb(realm) }
    private val deploymentImageDb by lazy { GuardianDeploymentImageDb(realm) }

    private var _profiles: List<GuardianProfile> = listOf()
    private var _profile: GuardianProfile? = null
    private var _deployment: GuardianDeployment? = null
    private var _deployLocation: DeploymentLocation? = null
    private var _configuration: GuardianConfiguration? = null
    private var _images: List<String> = listOf()
    private var _locate: Locate? = null

    private var useExistedLocation: Boolean = false

    private var _sampleRate = 24000

    private var latitude = 0.0
    private var longitude = 0.0

    private var lastCheckInTime: Long? = null

    private var prefsChanges = mapOf<String, String>()
    private var prefsEditor: SharedPreferences.Editor? = null

    private var currentCheck = 0
    private var currentCheckName = ""
    private var passedChecks = arrayListOf<Int>()

    private var onDeployClicked = false

    private lateinit var wifiHotspotManager: WifiHotspotManager
    private val analytics by lazy { Analytics(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_guardian_deployment)

        setupToolbar()

        val deploymentId = intent.extras?.getInt(EXTRA_DEPLOYMENT_ID)
        if (deploymentId != null) {
            val deployment = deploymentDb.getDeploymentById(deploymentId)
            if (deployment != null) {
                setDeployment(deployment)

                if (deployment.location != null) {
                    _deployLocation = deployment.location
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
            is GuardianConfigureFragment -> startFragment(GuardianSelectProfileFragment.newInstance())
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

    override fun isOpenedFromUnfinishedDeployment(): Boolean = false // guardian not have this feature so return false

    override fun getProfiles(): List<GuardianProfile> = _profiles

    override fun getProfile(): GuardianProfile? = _profile

    override fun setProfile(profile: GuardianProfile) {
        this._profile = profile
    }

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

    override fun setDeploymentConfigure(profile: GuardianProfile) {
        setProfile(profile)
        this._configuration = profile.asConfiguration()
        this._deployment?.configuration = _configuration

        // update deployment
        this._deployment?.let { deploymentDb.updateDeployment(it) }
        // update profile
        if (profile.name.isNotEmpty()) {
            profileDb.insertOrUpdateProfile(profile)
        }
    }

    override fun getConfiguration(): GuardianConfiguration? = _configuration

    override fun getSampleRate(): Int = _sampleRate

    override fun getWifiName(): String = _deployment?.wifiName ?: ""

    override fun getLastCheckInTime(): Long? = lastCheckInTime

    override fun getDeploymentLocation(): DeploymentLocation? = this._deployLocation

    override fun getLocationGroup(name: String): LocationGroups? {
        return locationGroupDb.getLocationGroup(name)
    }

    override fun setDeployLocation(locate: Locate, isExisted: Boolean) {
        val deployment = _deployment ?: GuardianDeployment()
        deployment.state = DeploymentState.Guardian.Locate.key // state

        this._deployLocation = locate.asDeploymentLocation()
        val deploymentId = deploymentDb.insertOrUpdateDeployment(deployment, _deployLocation!!)

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
            it.state = DeploymentState.Guardian.ReadyToUpload.key
            setDeployment(it)

            if (useExistedLocation) {
                this._locate?.let { locate ->
                    locateDb.insertOrUpdateLocate(it.id, locate, true) // update locate - last deployment
                }
            }

            deploymentImageDb.insertImage(it, _images)
            deploymentDb.updateDeployment(it)
            analytics.trackCreateGuardianDeploymentEvent()

            GuardianDeploymentSyncWorker.enqueue(this@GuardianDeploymentActivity)
            showComplete()
        }
    }

    override fun startSetupConfigure(profile: GuardianProfile) {
        setProfile(profile)
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
                this._profiles = profileDb.getProfiles()
                updateDeploymentState(DeploymentState.Guardian.Config)
                startFragment(GuardianSelectProfileFragment.newInstance())
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

    override fun startMapPicker(latitude: Double, longitude: Double, name: String) {
        setLatLng(latitude, longitude)
        startFragment(MapPickerFragment.newInstance(latitude, longitude, name))
    }

    private fun setLatLng(latitude: Double, longitude: Double) {
        this.latitude = latitude
        this.longitude = longitude
    }

    override fun startLocationPage(latitude: Double, longitude: Double, name: String) {
        startFragment(LocationFragment.newInstance(latitude, longitude, name))
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
