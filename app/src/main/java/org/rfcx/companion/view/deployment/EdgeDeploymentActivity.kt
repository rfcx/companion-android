package org.rfcx.companion.view.deployment

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.location.Location
import android.location.LocationManager
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import io.realm.Realm
import io.realm.RealmList
import kotlinx.android.synthetic.main.activity_deployment.*
import kotlinx.android.synthetic.main.toolbar_default.*
import org.rfcx.companion.R
import org.rfcx.companion.entity.*
import org.rfcx.companion.localdb.*
import org.rfcx.companion.localdb.guardian.GuardianDeploymentDb
import org.rfcx.companion.service.DeploymentSyncWorker
import org.rfcx.companion.service.DownloadStreamState
import org.rfcx.companion.service.DownloadStreamsWorker
import org.rfcx.companion.util.Analytics
import org.rfcx.companion.util.AudioMothChimeConnector
import org.rfcx.companion.util.Preferences
import org.rfcx.companion.util.Preferences.Companion.ENABLE_LOCATION_TRACKING
import org.rfcx.companion.util.RealmHelper
import org.rfcx.companion.util.geojson.GeoJsonUtils
import org.rfcx.companion.view.deployment.guardian.GuardianDeploymentActivity
import org.rfcx.companion.view.deployment.locate.LocationFragment
import org.rfcx.companion.view.deployment.locate.MapPickerFragment
import org.rfcx.companion.view.deployment.locate.SelectingExistedSiteFragment
import org.rfcx.companion.view.deployment.location.DetailDeploymentSiteFragment
import org.rfcx.companion.view.deployment.location.SetDeploymentSiteFragment
import org.rfcx.companion.view.deployment.sync.NewSyncFragment
import org.rfcx.companion.view.detail.MapPickerProtocol
import org.rfcx.companion.view.dialog.*
import java.util.*

class EdgeDeploymentActivity : AppCompatActivity(), EdgeDeploymentProtocol, CompleteListener,
    MapPickerProtocol {
    // manager database
    private val realm by lazy { Realm.getInstance(RealmHelper.migrationConfig()) }
    private val deploymentDb by lazy { EdgeDeploymentDb(realm) }
    private val guardianDeploymentDb by lazy { GuardianDeploymentDb(realm) }
    private val locateDb by lazy { LocateDb(realm) }
    private val locationGroupDb by lazy { LocationGroupDb(realm) }
    private val deploymentImageDb by lazy { DeploymentImageDb(realm) }
    private val trackingDb by lazy { TrackingDb(realm) }
    private val trackingFileDb by lazy { TrackingFileDb(realm) }

    private var _deployment: EdgeDeployment? = null
    private var _deployLocation: DeploymentLocation? = null
    private var _images: List<String> = listOf()
    private var _deployLocationGroup: LocationGroup? = null
    private var _locate: Locate? = null

    private var useExistedLocation: Boolean = false

    private var currentLocation: Location? = null

    private var audioMothConnector = AudioMothChimeConnector()
    private var calendar = Calendar.getInstance()

    private var latitude = 0.0
    private var longitude = 0.0
    private var altitude = 0.0
    private var nameLocation: String = ""

    private var currentCheck = 0
    private var currentCheckName = ""
    private var passedChecks = RealmList<Int>()

    private var needTone = true

    private val preferences = Preferences.getInstance(this)

    private val analytics by lazy { Analytics(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_deployment)

        setupToolbar()

        val deploymentId = intent.extras?.getInt(EXTRA_DEPLOYMENT_ID)
        if (deploymentId != null) {
            handleDeploymentStep(deploymentId)
        } else {
            openWithEdgeDevice()
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

    private fun saveImages(deployment: EdgeDeployment) {
        deploymentImageDb.deleteImages(deployment.id)
        deploymentImageDb.insertImage(deployment, null, _images)
    }

    override fun openWithEdgeDevice() {
        startCheckList()
    }

    override fun openWithGuardianDevice() {
        GuardianDeploymentActivity.startActivity(this)
        finish()
    }

    override fun isOpenedFromUnfinishedDeployment(): Boolean {
        return fromUnfinishedDeployment
    }

    override fun onSupportNavigateUp(): Boolean {
        backStep()
        return true
    }

    override fun nextStep() {
        if (passedChecks.contains(2) && _images.isNullOrEmpty()) {
            passedChecks.remove(2)
        }

        if (currentCheck !in passedChecks) {
            if (currentCheck == 2 && _images.isNullOrEmpty()) {
                startCheckList()
                return
            } else {
                passedChecks.add(currentCheck)
            }
        }
        startCheckList()
    }

    override fun backStep() {
        val container = supportFragmentManager.findFragmentById(R.id.contentContainer)
        when (container) {
            is MapPickerFragment -> startFragment(LocationFragment.newInstance())
            is EdgeCheckListFragment -> {
                _deployment?.let {
                    it.passedChecks = passedChecks
                    deploymentDb.updateDeployment(it)

                    saveImages(it)
                }
                passedChecks.clear() // remove all passed
                finish()
            }
            is LocationFragment -> {
                val isFragmentPopped = handleNestedFragmentBackStack(supportFragmentManager)
                if (!isFragmentPopped && supportFragmentManager.backStackEntryCount <= 1) {
                    // if top's fragment is  LocationFragment then finish else show LocationFragment fragment
                    when {
                        supportFragmentManager.fragments.firstOrNull() is LocationFragment -> {
                            startCheckList()
                        }
                        supportFragmentManager.fragments.lastOrNull() is LocationFragment -> {
                            startCheckList()
                        }
                        else -> {
                            startLocationPage(
                                this.latitude,
                                this.longitude,
                                this.altitude,
                                this.nameLocation,
                                true
                            )
                        }
                    }
                } else if (!isFragmentPopped) {
                    super.onBackPressed()
                }
            }
            is DetailDeploymentSiteFragment -> startFragment(SetDeploymentSiteFragment.newInstance())
            is ChooseDeviceFragment -> finish()
            else -> startCheckList()
        }
    }

    override fun startCheckList() {
        startFragment(EdgeCheckListFragment.newInstance())
    }

    override fun startSelectingExistedSite(latitude: Double, longitude: Double) {
        startFragment(SelectingExistedSiteFragment.newInstance(latitude, longitude))
    }

    override fun startDetailDeploymentSite(id: Int, name: String?, isNewSite: Boolean) {
        startFragment(DetailDeploymentSiteFragment.newInstance(id, name, isNewSite))
    }

    override fun getDeployment(): EdgeDeployment? {
        if (this._deployment == null) {
            this._deployment = EdgeDeployment()
        }
        return this._deployment
    }
    override fun getCurrentLocation(): Location = currentLocation ?: Location(LocationManager.GPS_PROVIDER)

    override fun getLocationGroup(name: String): LocationGroups? {
        return locationGroupDb.getLocationGroup(name)
    }

    override fun setDeployment(deployment: EdgeDeployment) {
        this._deployment = deployment
    }

    override fun setCurrentLocation(location: Location) {
        this.currentLocation = location
    }

    override fun showSyncInstruction() {
        val instructionDialog: SyncInstructionDialogFragment =
            supportFragmentManager.findFragmentByTag(TAG_SYNC_INSTRUCTION_DIALOG) as SyncInstructionDialogFragment?
                ?: run {
                    SyncInstructionDialogFragment()
                }
        instructionDialog.show(supportFragmentManager,
            TAG_SYNC_INSTRUCTION_DIALOG
        )
    }

    override fun showSiteLoadingDialog(text: String) {
        var siteLoadingDialog: SiteLoadingDialogFragment =
            supportFragmentManager.findFragmentByTag(TAG_SITE_LOADING_DIALOG) as SiteLoadingDialogFragment?
                ?: run {
                    SiteLoadingDialogFragment(text)
                }
        if (siteLoadingDialog.isAdded) {
            siteLoadingDialog.dismiss()
            siteLoadingDialog = SiteLoadingDialogFragment(text)
        }
        siteLoadingDialog.show(supportFragmentManager,
            TAG_SITE_LOADING_DIALOG
        )
    }

    override fun getDeploymentLocation(): DeploymentLocation? = this._deployLocation

    override fun setDeployLocation(locate: Locate, isExisted: Boolean) {
        val deployment = _deployment ?: EdgeDeployment()
        deployment.isActive = locate.serverId == null
        deployment.state = DeploymentState.Edge.Locate.key // state

        this._deployLocation = locate.asDeploymentLocation()
        val deploymentId = deploymentDb.insertOrUpdate(deployment, _deployLocation!!)

        useExistedLocation = isExisted
        this._locate = locate
        if (!useExistedLocation) {
            locateDb.insertOrUpdateLocate(deploymentId, locate) // update locate - last deployment
        }

        setDeployment(deployment)
    }

    override fun setImages(images: List<String>) {
        this._images = images
    }

    override fun getImages(): List<String> {
        return this._images
    }

    private fun setLatLng(latitude: Double, longitude: Double, altitude: Double, name: String) {
        this.latitude = latitude
        this.longitude = longitude
        this.altitude = altitude
        this.nameLocation = name
    }

    override fun setReadyToDeploy() {
        showLoading()
        _deployment?.let {
            it.deployedAt = Date()
            it.updatedAt = Date()
            it.isActive = true
            it.state = DeploymentState.Edge.ReadyToUpload.key
            setDeployment(it)

            if (useExistedLocation) {
                this._locate?.let { locate ->
                    locateDb.insertOrUpdateLocate(it.id, locate) // update locate - last deployment
                    val deployments = locate.serverId?.let { it1 -> deploymentDb.getDeploymentsBySiteId(it1) }
                    val guardianDeployments = locate.serverId?.let { it1 -> guardianDeploymentDb.getDeploymentsBySiteId(it1) }
                    deployments?.forEach { deployment ->
                        deploymentDb.updateIsActive(deployment.id)
                    }
                    guardianDeployments?.forEach { deployment ->
                        guardianDeploymentDb.updateIsActive(deployment.id)
                    }
                }
            }
            saveImages(it)
            deploymentDb.updateDeployment(it)

            //track getting
            if (preferences.getBoolean(ENABLE_LOCATION_TRACKING)) {
                val track = trackingDb.getFirstTracking()
                track?.let { t ->
                    val point = t.points.toListDoubleArray()
                    val trackingFile = TrackingFile(
                        deploymentId = it.id,
                        siteId = this._locate!!.id,
                        localPath = GeoJsonUtils.generateGeoJson(this, GeoJsonUtils.generateFileName(it.deployedAt, it.deploymentKey!!), point).absolutePath
                    )
                    trackingFileDb.insertOrUpdate(trackingFile)
                }
            }

            analytics.trackCreateAudiomothDeploymentEvent()

            DeploymentSyncWorker.enqueue(this@EdgeDeploymentActivity)
            hideLoading()
            showComplete()
        }
    }

    override fun handleCheckClicked(number: Int) {
        // setup fragment for current step
        currentCheck = number
        when (number) {
            0 -> {
                updateDeploymentState(DeploymentState.Edge.Locate)
                startFragment(SetDeploymentSiteFragment.newInstance())
            }
            1 -> {
                updateDeploymentState(DeploymentState.Edge.Sync)
                startFragment(NewSyncFragment.newInstance())
            }
            2 -> {
                updateDeploymentState(DeploymentState.Edge.Deploy)
                startFragment(DeployFragment.newInstance())
            }
        }
    }

    override fun getPassedChecks(): List<Int> = passedChecks

    override fun setCurrentPage(name: String) {
        currentCheckName = name
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

    override fun startSyncing(status: String) {
        startFragment(NewSyncFragment.newInstance())
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

    override fun playSyncSound() {
        val deploymentId = getDeployment()?.deploymentKey
        val deploymentIdArrayInt = deploymentId?.chunked(2)?.map { it.toInt(radix = 16) }?.toTypedArray() ?: arrayOf()
        Thread {
            for (i in 1..3){
                audioMothConnector.playTimeAndDeploymentID(
                    calendar,
                    deploymentIdArrayInt
                )
            }
            this@EdgeDeploymentActivity.runOnUiThread {
                startFragment(NewSyncFragment.newInstance(6))
            }
        }.start()
    }

    override fun playTone(duration: Int) {
        needTone = true
        Thread {
            var durationCount = 0
            val durationFrac = duration % TONE_DURATION
            do {
                durationCount += if (durationCount + durationFrac == duration) {
                    audioMothConnector.playTone(durationFrac)
                    durationFrac
                } else {
                    audioMothConnector.playTone(TONE_DURATION)
                    TONE_DURATION
                }
            } while (durationCount < duration && duration >= TONE_DURATION && needTone)
        }.start()
    }

    override fun stopPlaySound() {
        needTone = false
        audioMothConnector.stopPlay()
    }

    override fun isSiteLoading(): DownloadStreamState {
        return DownloadStreamsWorker.isRunning()
    }

    override fun startMapPicker(latitude: Double, longitude: Double, altitude: Double, name: String) {
        setLatLng(latitude, longitude, altitude, name)
        startFragment(MapPickerFragment.newInstance(latitude, longitude, altitude, name))
    }

    private fun handleDeploymentStep(deploymentId: Int) {
        val deployment = deploymentDb.getDeploymentById(deploymentId)
        if (deployment != null) {
            setDeployment(deployment)

            if (deployment.stream != null) {
                _deployLocation = deployment.stream
            }

            if (deployment.passedChecks != null) {
                val passedChecks = deployment.passedChecks
                this.passedChecks = passedChecks ?: RealmList<Int>()
            }

            val images = deploymentImageDb.getImageByDeploymentId(deployment.id)
            if (images.isNotEmpty()) {
                val localPaths = arrayListOf<String>()
                images.forEach {
                    localPaths.add(it.localPath)
                }
                _images = localPaths
            }

            currentCheck = if (deployment.state == 1) {
                deployment.state
            } else {
                deployment.state - 1
            }
            openWithEdgeDevice()
        }
    }

    private fun startFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(contentContainer.id, fragment)
            .commit()
    }

    private fun updateDeploymentState(state: DeploymentState.Edge) {
        this._deployment?.state = state.key
        this._deployment?.let { deploymentDb.updateDeployment(it) }
    }

    private fun showLoading() {
        val loadingDialog: LoadingDialogFragment =
            supportFragmentManager.findFragmentByTag(loadingDialogTag) as LoadingDialogFragment?
                ?: run {
                    LoadingDialogFragment()
                }
        loadingDialog.show(supportFragmentManager, loadingDialogTag)
    }

    private fun showComplete() {
        val completeFragment: CompleteFragment =
            supportFragmentManager.findFragmentByTag(CompleteFragment.tag) as CompleteFragment?
                ?: run {
                    CompleteFragment()
                }
        completeFragment.isCancelable = false
        completeFragment.show(supportFragmentManager, CompleteFragment.tag)
    }

    private fun hideLoading() {
        val loadingDialog: LoadingDialogFragment? =
            supportFragmentManager.findFragmentByTag(loadingDialogTag) as LoadingDialogFragment?
        loadingDialog?.dismissDialog()
    }

    override fun onAnimationEnd() {
        if(supportFragmentManager.fragments.last() is NewSyncFragment) {
            nextStep()
        } else {
            finish()
        }
    }

    override fun onBackPressed() {
        backStep()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        val fragment =
            supportFragmentManager.findFragmentByTag(LocationFragment.TAG) as LocationFragment?
                ?: LocationFragment.newInstance()
        fragment.onActivityResult(requestCode, resultCode, data)
    }

    private fun handleNestedFragmentBackStack(fragmentManager: FragmentManager): Boolean {
        val childFragmentList = fragmentManager.fragments
        if (childFragmentList.size > 0) {
            for (index in childFragmentList.size - 1 downTo 0) {
                val fragment = childFragmentList[index]
                val isPopped = handleNestedFragmentBackStack(fragment.childFragmentManager)
                return when {
                    isPopped -> true
                    fragmentManager.backStackEntryCount > 0 -> {
                        fragmentManager.popBackStack()
                        true
                    }
                    else -> false
                }
            }
        }
        return false
    }

    override fun onDestroy() {
        super.onDestroy()
        fromUnfinishedDeployment = false
    }

    companion object {
        const val loadingDialogTag = "LoadingDialog"
        const val TAG_SYNC_INSTRUCTION_DIALOG = "SyncInstructionDialogFragment"
        const val TAG_SITE_LOADING_DIALOG = "SiteLoadingDialogFragment"
        const val EXTRA_DEPLOYMENT_ID = "EXTRA_DEPLOYMENT_ID"
        const val TONE_DURATION = 10000

        private var fromUnfinishedDeployment = false

        fun startActivity(context: Context) {
            val intent = Intent(context, EdgeDeploymentActivity::class.java)
            context.startActivity(intent)
        }

        fun startActivity(context: Context, deploymentId: Int) {
            val intent = Intent(context, EdgeDeploymentActivity::class.java)
            intent.putExtra(EXTRA_DEPLOYMENT_ID, deploymentId)
            context.startActivity(intent)
        }

        fun startActivity(context: Context, deploymentId: Int, requestCode: Int) {
            val intent = Intent(context, EdgeDeploymentActivity::class.java)
            intent.putExtra(EXTRA_DEPLOYMENT_ID, deploymentId)
            fromUnfinishedDeployment = true
            (context as Activity).startActivityForResult(intent, requestCode)
        }
    }
}
