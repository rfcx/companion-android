package org.rfcx.companion.view.deployment.songmeter

import android.content.Context
import android.content.Intent
import android.location.Location
import android.location.LocationManager
import android.os.Bundle
import android.view.Menu
import android.view.View
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.gson.Gson
import io.realm.RealmList
import kotlinx.android.synthetic.main.toolbar_default.*
import org.rfcx.companion.R
import org.rfcx.companion.base.ViewModelFactory
import org.rfcx.companion.entity.*
import org.rfcx.companion.entity.guardian.Deployment
import org.rfcx.companion.repo.api.CoreApiHelper
import org.rfcx.companion.repo.api.CoreApiServiceImpl
import org.rfcx.companion.repo.api.DeviceApiHelper
import org.rfcx.companion.repo.api.DeviceApiServiceImpl
import org.rfcx.companion.repo.ble.BleConnectDelegate
import org.rfcx.companion.repo.ble.BleDetectService
import org.rfcx.companion.repo.ble.BleHelper
import org.rfcx.companion.repo.local.LocalDataHelper
import org.rfcx.companion.service.DeploymentSyncWorker
import org.rfcx.companion.service.DownloadStreamState
import org.rfcx.companion.service.DownloadStreamsWorker
import org.rfcx.companion.util.Analytics
import org.rfcx.companion.util.Preferences
import org.rfcx.companion.util.geojson.GeoJsonUtils
import org.rfcx.companion.util.getListSite
import org.rfcx.companion.view.deployment.BaseDeploymentActivity
import org.rfcx.companion.view.deployment.DeployFragment
import org.rfcx.companion.view.deployment.locate.MapPickerFragment
import org.rfcx.companion.view.deployment.location.DetailDeploymentSiteFragment
import org.rfcx.companion.view.deployment.location.SetDeploymentSiteFragment
import org.rfcx.companion.view.deployment.songmeter.detect.SongMeterDetectFragment
import org.rfcx.companion.view.deployment.songmeter.viewmodel.SongMeterViewModel
import org.rfcx.companion.view.dialog.CompleteFragment
import org.rfcx.companion.view.dialog.LoadingDialogFragment
import java.util.*

class SongMeterDeploymentActivity : BaseDeploymentActivity(), SongMeterDeploymentProtocol {

    private var currentCheck = 0
    private var passedChecks = RealmList<Int>()
    private var useExistedLocation: Boolean = false
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    private lateinit var songMeterViewModel: SongMeterViewModel

    private var deployments = listOf<Deployment>()
    private var sites = listOf<Stream>()
    private var stream: Stream? = null

    private var songMeterId: String? = null

    private val preferences = Preferences.getInstance(this)

    private val analytics by lazy { Analytics(this) }

    private var menuAll: Menu? = null

    companion object {
        const val TAG = "SongMeterDeploymentActivity"
        const val loadingDialogTag = "LoadingDialog"
        const val TAG_HELP_DIALOG = "SongMeterHelpDialogFragment"
        const val TAG_SITE_LOADING_DIALOG = "SiteLoadingDialogFragment"

        fun startActivity(context: Context) {
            val intent = Intent(context, SongMeterDeploymentActivity::class.java)
            context.startActivity(intent)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_song_meter_deployment)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        setupToolbar()
        startCheckList()
        setViewModel()
        setObserver()
        preferences.clearSelectedProject()

        fusedLocationClient.lastLocation
            .addOnSuccessListener { location: Location? ->
                location?.let { setCurrentLocation(it) }
            }
    }

    private fun setViewModel() {
        songMeterViewModel = ViewModelProvider(
            this,
            ViewModelFactory(
                application,
                DeviceApiHelper(DeviceApiServiceImpl(this)),
                CoreApiHelper(CoreApiServiceImpl(this)),
                LocalDataHelper(),
                BleHelper(BleDetectService(this), BleConnectDelegate(this))
            )
        ).get(SongMeterViewModel::class.java)
    }

    private fun setObserver() {
        songMeterViewModel.getDeployments().observe(
            this,
            Observer {
                this.deployments = it.filter { deployment -> deployment.isCompleted() }
                setSiteItems()
            }
        )

        songMeterViewModel.getSites().observe(
            this,
            Observer {
                this.sites = it
                setSiteItems()
            }
        )
    }

    override fun setSongMeterId(id: String) {
        songMeterId = id
    }

    override fun getDeployment(): Deployment? {
        if (this._deployment == null) {
            val dp = Deployment()
            dp.device = Device.SONGMETER.value
            this._deployment = dp
        }
        return this._deployment
    }

    override fun setDeployment(deployment: Deployment) {
        this._deployment = deployment
    }

    override fun startCheckList() {
        startFragment(SongMeterCheckListFragment.newInstance())
    }

    override fun nextStep() {
        if (currentCheck !in passedChecks) {
            passedChecks.add(currentCheck)
        }
        currentCheck = -1 // reset check
        startCheckList()
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

    override fun backStep() {
        when (supportFragmentManager.findFragmentById(R.id.contentContainer)) {
            is MapPickerFragment -> startFragment(
                DetailDeploymentSiteFragment.newInstance(
                    latitude,
                    longitude,
                    siteId,
                    streamName
                )
            )
            is SongMeterCheckListFragment -> {
                _deployment?.let {
                    it.passedChecks = passedChecks
                    songMeterViewModel.updateDeployment(it)
                    saveImages(it)
                }
                passedChecks.clear() // remove all passed
                finish()
            }
            is DetailDeploymentSiteFragment -> {
                if (stream == null) {
                    startFragment(
                        SetDeploymentSiteFragment.newInstance(
                            currentLocate?.latitude ?: 0.0, currentLocate?.longitude ?: 0.0
                        )
                    )
                } else {
                    startCheckList()
                }
            }
            else -> startCheckList()
        }
    }

    override fun getStream(id: Int): Stream? {
        return songMeterViewModel.getStreamById(id)
    }

    override fun getProject(id: Int): Project? {
        return songMeterViewModel.getProjectById(id)
    }

    override fun onBackPressed() {
        backStep()
    }

    override fun onSupportNavigateUp(): Boolean {
        backStep()
        return true
    }

    private fun saveImages(deployment: Deployment) {
        songMeterViewModel.deleteImages(deployment)
        songMeterViewModel.insertImage(deployment, _images)
    }

    override fun setDeployLocation(stream: Stream, isExisted: Boolean) {
        val deployment = _deployment ?: Deployment()
        deployment.device = Device.SONGMETER.value
        deployment.isActive = stream.serverId == null
        deployment.state = DeploymentState.SongMeter.Locate.key // state

        this._stream = stream
        useExistedLocation = isExisted

        setDeployment(deployment)
    }

    override fun setReadyToDeploy() {
        showLoading()
        _deployment?.let {
            it.deployedAt = Date()
            it.updatedAt = Date()
            it.isActive = true
            it.state = DeploymentState.SongMeter.ReadyToUpload.key
            it.deviceParameters = Gson().toJson(SongMeterParameters(songMeterId))
            it.deploymentKey = songMeterId!!
            setDeployment(it)

            // set all deployments in stream to active false
            if (useExistedLocation) {
                this._stream?.let { locate ->
                    locate.deployments?.forEach { dp ->
                        songMeterViewModel.deleteImages(dp)
                        songMeterViewModel.deleteDeployment(dp.id)
                    }
                }
            }

            this._stream?.let { loc ->
                val streamId = songMeterViewModel.insertOrUpdate(loc)
                val deploymentId =
                    songMeterViewModel.insertOrUpdateDeployment(it, streamId)
                songMeterViewModel.updateDeploymentIdOnStream(
                    deploymentId,
                    streamId
                ) // update locate - last deployment
            }
            saveImages(it)

            // track getting
            if (preferences.getBoolean(Preferences.ENABLE_LOCATION_TRACKING)) {
                val track = songMeterViewModel.getFirstTracking()
                track?.let { t ->
                    val point = t.points.toListDoubleArray()
                    val trackingFile = TrackingFile(
                        deploymentId = it.id,
                        siteId = this.stream!!.id,
                        localPath = GeoJsonUtils.generateGeoJson(
                            this,
                            GeoJsonUtils.generateFileName(it.deployedAt, it.deploymentKey),
                            point
                        ).absolutePath
                    )
                    songMeterViewModel.insertOrUpdateTrackingFile(trackingFile)
                }
            }

            analytics.trackCreateSongMeterDeploymentEvent()

            DeploymentSyncWorker.enqueue(this)
            hideLoading()
            showComplete()
        }
    }

    private fun showLoading() {
        val loadingDialog: LoadingDialogFragment =
            supportFragmentManager.findFragmentByTag(loadingDialogTag) as LoadingDialogFragment?
                ?: run {
                    LoadingDialogFragment()
                }
        if (loadingDialog.isVisible || loadingDialog.isAdded) return
        loadingDialog.show(supportFragmentManager, loadingDialogTag)
    }

    private fun hideLoading() {
        val loadingDialog: LoadingDialogFragment =
            supportFragmentManager.findFragmentByTag(loadingDialogTag) as LoadingDialogFragment?
                ?: return
        if (!loadingDialog.isVisible || !loadingDialog.isAdded) return
        loadingDialog.dismissDialog()
    }

    private fun showComplete() {
        val completeFragment: CompleteFragment =
            supportFragmentManager.findFragmentByTag(CompleteFragment.tag) as CompleteFragment?
                ?: run {
                    CompleteFragment()
                }
        completeFragment.isCancelable = false
        if (!completeFragment.isVisible) completeFragment.show(supportFragmentManager, CompleteFragment.tag)
    }

    override fun setCurrentLocation(location: Location) {
        this.currentLocate = location
    }

    override fun handleCheckClicked(number: Int) {
        // setup fragment for current step
        currentCheck = number
        when (number) {
            0 -> {
                val site = this.stream
                if (site == null) {
                    startFragment(
                        SetDeploymentSiteFragment.newInstance(
                            currentLocate?.latitude ?: 0.0, currentLocate?.longitude ?: 0.0
                        )
                    )
                } else {
                    startDetailDeploymentSite(site.id, site.name, false)
                }
            }
            1 -> {
                startFragment(SongMeterDetectFragment.newInstance())
            }
            2 -> {
                startFragment(DeployFragment.newInstance(Screen.SONG_METER_CHECK_LIST.id))
            }
        }
    }

    override fun getPassedChecks(): List<Int> = passedChecks

    override fun setCurrentPage(name: String) {
        currentCheckName = name
    }

    override fun showToolbar() {
        toolbar?.visibility = View.VISIBLE
    }

    override fun hideToolbar() {
        toolbar?.visibility = View.GONE
    }

    override fun setMenuToolbar(isVisibility: Boolean) {
        // do nothing
    }

    override fun setToolbarTitle() {
        supportActionBar?.apply {
            title = currentCheckName
        }
    }

    override fun setToolbarSubtitle(sub: String) {
        supportActionBar?.apply {
            subtitle = sub
        }
    }

    override fun isSiteLoading(): DownloadStreamState {
        return DownloadStreamsWorker.isRunning()
    }

    private fun setupToolbar() {
        setSupportActionBar(toolbar)
        supportActionBar?.apply {
            setDisplayShowTitleEnabled(true)
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowHomeEnabled(true)
        }
    }

    override fun onSelectedLocation(
        latitude: Double,
        longitude: Double,
        siteId: Int,
        name: String
    ) {
        startFragment(
            DetailDeploymentSiteFragment.newInstance(
                latitude,
                longitude,
                siteId,
                name,
                true
            )
        )
    }
}
