package org.rfcx.companion.view.deployment.songmeter

import android.content.Context
import android.content.Intent
import android.location.Location
import android.location.LocationManager
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import io.realm.RealmList
import kotlinx.android.synthetic.main.activity_song_meter_deployment.*
import kotlinx.android.synthetic.main.toolbar_default.*
import org.rfcx.companion.R
import org.rfcx.companion.base.ViewModelFactory
import org.rfcx.companion.entity.*
import org.rfcx.companion.entity.songmeter.Advertisement
import org.rfcx.companion.repo.api.DeviceApiHelper
import org.rfcx.companion.repo.api.DeviceApiServiceImpl
import org.rfcx.companion.repo.ble.BleDetectService
import org.rfcx.companion.repo.ble.BleHelper
import org.rfcx.companion.repo.local.LocalDataHelper
import org.rfcx.companion.service.DownloadStreamState
import org.rfcx.companion.util.getLastLocation
import org.rfcx.companion.util.getListSite
import org.rfcx.companion.view.deployment.DeployFragment
import org.rfcx.companion.view.deployment.locate.MapPickerFragment
import org.rfcx.companion.view.deployment.locate.SiteWithLastDeploymentItem
import org.rfcx.companion.view.deployment.location.DetailDeploymentSiteFragment
import org.rfcx.companion.view.deployment.location.SetDeploymentSiteFragment
import org.rfcx.companion.view.deployment.songmeter.connect.SongMeterConnectFragment
import org.rfcx.companion.view.deployment.songmeter.detect.SongMeterDetectFragment
import org.rfcx.companion.view.deployment.songmeter.viewmodel.SongMeterViewModel
import org.rfcx.companion.view.detail.MapPickerProtocol

class SongMeterDeploymentActivity : AppCompatActivity(), SongMeterDeploymentProtocol,
    MapPickerProtocol {

    private var _deployment: Deployment? = null
    private var _deployLocation: DeploymentLocation? = null
    private var _images: List<String> = listOf()
    private var _siteItems = arrayListOf<SiteWithLastDeploymentItem>()
    private var _locate: Locate? = null

    private var currentCheck = 0
    private var currentCheckName = ""
    private var passedChecks = RealmList<Int>()
    private var currentLocation: Location? = null
    private var useExistedLocation: Boolean = false

    private var latitude = 0.0
    private var longitude = 0.0
    private var siteId: Int = 0
    private var nameLocation: String = ""

    private lateinit var songMeterViewModel: SongMeterViewModel

    companion object {
        const val TAG = "SongMeterDeploymentActivity"

        fun startActivity(context: Context) {
            val intent = Intent(context, SongMeterDeploymentActivity::class.java)
            context.startActivity(intent)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_song_meter_deployment)
        this.currentLocation = this.getLastLocation()
        setupToolbar()
        startCheckList()
        setViewModel()
        setSiteItems()
    }

    private fun setViewModel() {
        songMeterViewModel = ViewModelProvider(
            this,
            ViewModelFactory(
                application,
                DeviceApiHelper(DeviceApiServiceImpl()),
                LocalDataHelper(),
                BleHelper(BleDetectService(this))
            )
        ).get(SongMeterViewModel::class.java)
    }

    override fun getDeployment(): Deployment? {
        if (this._deployment == null) {
            this._deployment = Deployment()
        }
        return this._deployment
    }

    override fun setDeployment(deployment: Deployment) {
        this._deployment = deployment
    }

    override fun redirectToConnectSongmeter(advertisement: Advertisement) {
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

    override fun isOpenedFromUnfinishedDeployment(): Boolean {
        TODO("Not yet implemented")
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

    private fun setSiteItems() {
        val loc = Location(LocationManager.GPS_PROVIDER)
        loc.latitude = 0.0
        loc.longitude = 0.0

        _siteItems = getListSite(
            this,
            songMeterViewModel.getDeploymentsFromLocal(),
            songMeterViewModel.getGuardianDeploymentsFromLocal(),
            getString(R.string.none),
            currentLocation ?: loc,
            songMeterViewModel.getLocatesFromLocal()
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
        songMeterViewModel.insertImage(deployment, null, _images)
    }

    override fun getDeploymentLocation(): DeploymentLocation? = this._deployLocation

    override fun getSiteItem(): ArrayList<SiteWithLastDeploymentItem> = this._siteItems

    override fun getLocationGroup(name: String): Project? {
        return songMeterViewModel.getProjectByName(name)
    }

    override fun getImages(): List<String> {
        return this._images
    }

    override fun getCurrentLocation(): Location {
        TODO("Not yet implemented")
    }

    override fun setDeployLocation(locate: Locate, isExisted: Boolean) {
        val deployment = _deployment ?: Deployment()
        deployment.isActive = locate.serverId == null
        deployment.state = DeploymentState.Edge.Locate.key // state

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
        TODO("Not yet implemented")
    }

    override fun setCurrentLocation(location: Location) {
        TODO("Not yet implemented")
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
        TODO("Not yet implemented")
    }

    override fun showSiteLoadingDialog(text: String) {
        TODO("Not yet implemented")
    }

    private fun startFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(contentContainer.id, fragment)
            .commit()
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
