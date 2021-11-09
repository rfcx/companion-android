package org.rfcx.companion.view.deployment.songmeter

import android.content.Context
import android.content.Intent
import android.location.Location
import android.location.LocationManager
import android.os.Bundle
import android.view.View
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.Observer
import io.realm.RealmList
import kotlinx.android.synthetic.main.toolbar_default.*
import org.rfcx.companion.R
import org.rfcx.companion.base.ViewModelFactory
import org.rfcx.companion.entity.*
import org.rfcx.companion.entity.guardian.Deployment
import org.rfcx.companion.entity.songmeter.Advertisement
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
import org.rfcx.companion.util.getLastLocation
import org.rfcx.companion.util.getListSite
import org.rfcx.companion.view.deployment.BaseDeploymentActivity
import org.rfcx.companion.view.deployment.DeployFragment
import org.rfcx.companion.view.deployment.locate.MapPickerFragment
import org.rfcx.companion.view.deployment.locate.SiteWithLastDeploymentItem
import org.rfcx.companion.view.deployment.location.DetailDeploymentSiteFragment
import org.rfcx.companion.view.deployment.location.SetDeploymentSiteFragment
import org.rfcx.companion.view.deployment.songmeter.connect.SongMeterConnectFragment
import org.rfcx.companion.view.deployment.songmeter.detect.SongMeterDetectFragment
import org.rfcx.companion.view.deployment.songmeter.viewmodel.SongMeterViewModel
import org.rfcx.companion.view.dialog.CompleteFragment
import org.rfcx.companion.view.dialog.LoadingDialogFragment
import org.rfcx.companion.view.dialog.SiteLoadingDialogFragment
import java.util.*

class SongMeterDeploymentActivity : BaseDeploymentActivity(), SongMeterDeploymentProtocol{

    private var currentCheck = 0
    private var passedChecks = RealmList<Int>()
    private var currentLocation: Location? = null
    private var useExistedLocation: Boolean = false

    private lateinit var songMeterViewModel: SongMeterViewModel

    private var deployments = listOf<Deployment>()
    private var sites = listOf<Locate>()

    private val preferences = Preferences.getInstance(this)

    private val analytics by lazy { Analytics(this) }

    companion object {
        const val TAG = "SongMeterDeploymentActivity"
        const val loadingDialogTag = "LoadingDialog"
        const val TAG_SITE_LOADING_DIALOG = "SiteLoadingDialogFragment"

        fun startActivity(context: Context) {
            val intent = Intent(context, SongMeterDeploymentActivity::class.java)
            context.startActivity(intent)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_song_meter_deployment)

        setupToolbar()
        startCheckList()
        setViewModel()
        setObserver()
        this.currentLocation = this.getLastLocation()
        preferences.clearSelectedProject()
    }

    private fun setViewModel() {
        songMeterViewModel = ViewModelProvider(
            this,
            ViewModelFactory(
                application,
                DeviceApiHelper(DeviceApiServiceImpl()),
                LocalDataHelper(),
                BleHelper(BleDetectService(this), BleConnectDelegate(this))
            )
        ).get(SongMeterViewModel::class.java)
    }

    private fun setObserver() {
        songMeterViewModel.getDeployments().observe(this, Observer {
            this.deployments = it.filter { deployment -> deployment.isCompleted() }
            setSiteItems()
        })

        songMeterViewModel.getSites().observe(this, Observer {
            this.sites = it
            setSiteItems()
        })
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

    override fun redirectToConnectSongMeter(advertisement: Advertisement) {
        startFragment(SongMeterConnectFragment.newInstance(advertisement))
    }

    override fun startMapPicker(latitude: Double, longitude: Double, siteId: Int, name: String) {
        setLatLng(latitude, longitude, siteId, name)
        startFragment(MapPickerFragment.newInstance(latitude, longitude, siteId, name))
    }

    private fun setLatLng(latitude: Double, longitude: Double, siteId: Int, name: String) {
        this.latitude = latitude
        this.longitude = longitude
        this.siteId = siteId
        this.nameLocation = name
    }

    override fun startCheckList() {
        startFragment(SongMeterCheckListFragment.newInstance())
    }

    override fun startDetailDeploymentSite(id: Int, name: String?, isNewSite: Boolean) {
        startFragment(DetailDeploymentSiteFragment.newInstance(id, name, isNewSite))
    }

    override fun isOpenedFromUnfinishedDeployment(): Boolean = false

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

    private fun setSiteItems() {
        val loc = Location(LocationManager.GPS_PROVIDER)
        loc.latitude = 0.0
        loc.longitude = 0.0

        _siteItems = getListSite(
            this,
            deployments,
            getString(R.string.none),
            currentLocation ?: loc,
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
                    nameLocation
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
                if (_deployLocation == null) {
                    startFragment(
                        SetDeploymentSiteFragment.newInstance(
                            currentLocation?.latitude ?: 0.0, currentLocation?.longitude ?: 0.0
                        )
                    )
                } else {
                    startCheckList()
                }
            }
            else -> startCheckList()
        }
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

    override fun getDeploymentLocation(): DeploymentLocation? = this._deployLocation

    override fun getSiteItem(): ArrayList<SiteWithLastDeploymentItem> = this._siteItems

    override fun getLocationGroup(name: String): Project? {
        return songMeterViewModel.getProjectByName(name)
    }

    override fun getImages(): List<String> {
        return this._images
    }

    override fun getCurrentLocation(): Location = currentLocation ?: Location(LocationManager.GPS_PROVIDER)

    override fun setDeployLocation(locate: Locate, isExisted: Boolean) {
        val deployment = _deployment ?: Deployment()
        deployment.device = Device.SONGMETER.value
        deployment.isActive = locate.serverId == null
        deployment.state = DeploymentState.SongMeter.Locate.key // state

        this._deployLocation = locate.asDeploymentLocation()
        this._locate = locate
        useExistedLocation = isExisted
        if (!useExistedLocation) {
            songMeterViewModel.setLocateInsertOrUpdate(locate)
        }

        setDeployment(deployment)
    }

    override fun setSiteItem(items: ArrayList<SiteWithLastDeploymentItem>) {
        this._siteItems = items
    }

    override fun setImages(images: List<String>) {
        this._images = images
    }

    override fun setReadyToDeploy() {
        showLoading()
        _deployment?.let {
            it.deployedAt = Date()
            it.updatedAt = Date()
            it.isActive = true
            it.state = DeploymentState.SongMeter.ReadyToUpload.key
            setDeployment(it)

            val deploymentId = songMeterViewModel.insertOrUpdateDeployment(it, _deployLocation!!)
            this._locate?.let { loc ->
                songMeterViewModel.insetOrUpdateStream(deploymentId, loc) // update locate - last deployment
            }

            if (useExistedLocation) {
                this._locate?.let { locate ->
                    val deployments =
                        locate.serverId?.let { it1 ->
                            songMeterViewModel.getDeploymentsBySiteId(
                                it1
                            )
                        }
                    deployments?.forEach { deployment ->
                        songMeterViewModel.updateIsActive(deployment.id)
                    }
                }
            }
            saveImages(it)

            //track getting
            if (preferences.getBoolean(Preferences.ENABLE_LOCATION_TRACKING)) {
                val track = songMeterViewModel.getFirstTracking()
                track?.let { t ->
                    val point = t.points.toListDoubleArray()
                    val trackingFile = TrackingFile(
                        deploymentId = it.id,
                        siteId = this._locate!!.id,
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
        loadingDialog.show(supportFragmentManager, loadingDialogTag)
    }

    private fun hideLoading() {
        val loadingDialog: LoadingDialogFragment? =
            supportFragmentManager.findFragmentByTag(loadingDialogTag) as LoadingDialogFragment?
        loadingDialog?.dismissDialog()
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


    override fun setCurrentLocation(location: Location) {
        this.currentLocation = location
    }

    override fun handleCheckClicked(number: Int) {
        // setup fragment for current step
        currentCheck = number
        when (number) {
            0 -> {
                val site = this._locate
                if (site == null) {
                    startFragment(
                        SetDeploymentSiteFragment.newInstance(
                            currentLocation?.latitude ?: 0.0, currentLocation?.longitude ?: 0.0
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
        siteLoadingDialog.show(
            supportFragmentManager,
            TAG_SITE_LOADING_DIALOG
        )
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
