package org.rfcx.companion.view.deployment

import android.content.Context
import android.content.Intent
import android.location.Location
import android.location.LocationManager
import android.os.Bundle
import androidx.lifecycle.ViewModelProvider
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
import org.rfcx.companion.repo.local.LocalDataHelper
import org.rfcx.companion.service.DeploymentSyncWorker
import org.rfcx.companion.util.Analytics
import org.rfcx.companion.util.Preferences
import org.rfcx.companion.util.Preferences.Companion.ENABLE_LOCATION_TRACKING
import org.rfcx.companion.util.geojson.GeoJsonUtils
import org.rfcx.companion.util.getListSite
import org.rfcx.companion.view.deployment.locate.MapPickerFragment
import org.rfcx.companion.view.deployment.location.DetailDeploymentSiteFragment
import org.rfcx.companion.view.deployment.location.SetDeploymentSiteFragment
import org.rfcx.companion.view.deployment.sync.NewSyncFragment
import org.rfcx.companion.view.dialog.CompleteFragment
import org.rfcx.companion.view.dialog.LoadingDialogFragment
import java.util.*

class AudioMothDeploymentActivity : BaseDeploymentActivity(), AudioMothDeploymentProtocol {
    private lateinit var audioMothDeploymentViewModel: AudioMothDeploymentViewModel
    private var useExistedLocation: Boolean = false

    private var currentCheck = 0
    private var passedChecks = RealmList<Int>()
    private var needTone = true

    private val preferences = Preferences.getInstance(this)

    private val analytics by lazy { Analytics(this) }

    private var deployments = listOf<Deployment>()
    private var sites = listOf<Stream>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_deployment)

        setupToolbar()
        startCheckList()
        setViewModel()
        setObserver()
        preferences.clearSelectedProject()
    }

    private fun setViewModel() {
        audioMothDeploymentViewModel = ViewModelProvider(
            this,
            ViewModelFactory(
                application,
                DeviceApiHelper(DeviceApiServiceImpl(this)),
                CoreApiHelper(CoreApiServiceImpl(this)),
                LocalDataHelper()
            )
        ).get(AudioMothDeploymentViewModel::class.java)
    }

    private fun setObserver() {
        audioMothDeploymentViewModel.getDeployments().observe(
            this
        ) {
            this.deployments = it.filter { deployment -> deployment.isCompleted() }
            setSiteItems()
        }

        audioMothDeploymentViewModel.getSites().observe(
            this
        ) {
            this.sites = it
            setSiteItems()
        }
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

    private fun setupToolbar() {
        setSupportActionBar(toolbar)
        supportActionBar?.apply {
            setDisplayShowTitleEnabled(true)
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowHomeEnabled(true)
        }
    }

    private fun saveImages(deployment: Deployment) {
        audioMothDeploymentViewModel.deleteImages(deployment.id)
        audioMothDeploymentViewModel.insertImage(
            deployment,
            _images.filter { it.path != null }.map { it.path!! }
        )
    }

    override fun onSupportNavigateUp(): Boolean {
        backStep()
        return true
    }

    override fun nextStep() {
        if (currentCheck !in passedChecks) {
            passedChecks.add(currentCheck)
        }
        currentCheck = -1 // reset check
        startCheckList()
    }

    override fun backStep() {
        val container = supportFragmentManager.findFragmentById(R.id.contentContainer)
        when (container) {
            is MapPickerFragment -> startFragment(
                DetailDeploymentSiteFragment.newInstance(
                    latitude,
                    longitude,
                    siteId
                )
            )
            is AudioMothCheckListFragment -> {
                passedChecks.clear() // remove all passed
                finish()
            }
            is DetailDeploymentSiteFragment -> {
                if (_stream == null) {
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
        return audioMothDeploymentViewModel.getStreamById(id)
    }

    override fun startCheckList() {
        startFragment(AudioMothCheckListFragment.newInstance())
    }

    override fun getDeployment(): Deployment? {
        if (this._deployment == null) {
            val dp = Deployment()
            dp.device = Device.AUDIOMOTH.value
            this._deployment = dp
        }
        return this._deployment
    }

    override fun getProject(id: Int): Project? {
        return audioMothDeploymentViewModel.getProjectById(id)
    }

    override fun setDeployment(deployment: Deployment) {
        this._deployment = deployment
    }

    override fun setDeployLocation(stream: Stream, isExisted: Boolean) {
        val deployment = _deployment ?: Deployment()
        deployment.device = Device.AUDIOMOTH.value
        deployment.isActive = stream.serverId == null
        deployment.state = DeploymentState.AudioMoth.Locate.key // state

        this._stream = stream
        useExistedLocation = isExisted

        setDeployment(deployment)
    }

    override fun setReadyToDeploy() {
        showLoading()
        _deployment?.let { it ->
            it.deployedAt = Date()
            it.updatedAt = Date()
            it.isActive = true
            it.state = DeploymentState.AudioMoth.ReadyToUpload.key
            setDeployment(it)

            // set all deployments in stream to active false
            if (useExistedLocation) {
                this._stream?.let { locate ->
                    locate.deployments?.forEach { dp ->
                        audioMothDeploymentViewModel.updateIsActive(dp.id)
                    }
                }
            }

            this._stream?.let { loc ->
                val streamId = audioMothDeploymentViewModel.insertOrUpdate(loc)
                val deploymentId =
                    audioMothDeploymentViewModel.insertOrUpdateDeployment(it, streamId)
                audioMothDeploymentViewModel.updateDeploymentIdOnStream(
                    deploymentId,
                    streamId
                ) // update locate - last deployment
            }
            saveImages(it)

            // track getting
            if (preferences.getBoolean(ENABLE_LOCATION_TRACKING)) {
                val track = audioMothDeploymentViewModel.getFirstTracking()
                track?.let { t ->
                    val point = t.points.toListDoubleArray()
                    val trackingFile = TrackingFile(
                        deploymentId = it.id,
                        siteId = this._stream!!.id,
                        localPath = GeoJsonUtils.generateGeoJson(
                            this,
                            GeoJsonUtils.generateFileName(it.deployedAt, it.deploymentKey),
                            point
                        ).absolutePath
                    )
                    audioMothDeploymentViewModel.insertOrUpdateTrackingFile(trackingFile)
                }
            }

            analytics.trackCreateAudiomothDeploymentEvent()

            DeploymentSyncWorker.enqueue(this@AudioMothDeploymentActivity)
            hideLoading()
            showComplete()
        }
    }

    override fun handleCheckClicked(number: Int) {
        // setup fragment for current step
        currentCheck = number
        when (number) {
            0 -> {
                updateDeploymentState(DeploymentState.AudioMoth.Locate)
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
            1 -> {
                updateDeploymentState(DeploymentState.AudioMoth.Sync)
                startFragment(NewSyncFragment.newInstance())
            }
            2 -> {
                updateDeploymentState(DeploymentState.AudioMoth.Deploy)
                startFragment(DeployFragment.newInstance(Screen.AUDIO_MOTH_CHECK_LIST.id))
            }
        }
    }

    override fun getPassedChecks(): List<Int> = passedChecks

    override fun setMenuToolbar(isVisibility: Boolean) {}

    override fun setToolbarSubtitle(sub: String) {
        supportActionBar?.apply {
            subtitle = sub
        }
    }

    override fun startSyncing(status: String) {
        startFragment(NewSyncFragment.newInstance())
    }

    override fun playSyncSound() {
        val deploymentIdArrayInt =
            getDeployment()?.deploymentKey?.chunked(2)?.map { it.toInt(radix = 16) }?.toTypedArray()
                ?: arrayOf()
        Thread {
            audioMothDeploymentViewModel.playSyncSound(Calendar.getInstance(), deploymentIdArrayInt)
            this@AudioMothDeploymentActivity.runOnUiThread {
                val fragment =
                    supportFragmentManager.findFragmentById(R.id.contentContainer)
                if (fragment is NewSyncFragment)
                    fragment.showRepeatSync()
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
                    audioMothDeploymentViewModel.playTone(durationFrac)
                    durationFrac
                } else {
                    audioMothDeploymentViewModel.playTone(TONE_DURATION)
                    TONE_DURATION
                }
            } while (durationCount < duration && duration >= TONE_DURATION && needTone)
        }.start()
    }

    override fun stopPlaySound() {
        needTone = false
        audioMothDeploymentViewModel.stopPlaySound()
    }

    private fun updateDeploymentState(state: DeploymentState.AudioMoth) {
        this._deployment?.state = state.key
        this._deployment?.let { audioMothDeploymentViewModel.updateDeployment(it) }
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

    private fun showComplete() {
        val completeFragment: CompleteFragment =
            supportFragmentManager.findFragmentByTag(CompleteFragment.tag) as CompleteFragment?
                ?: run {
                    CompleteFragment()
                }
        completeFragment.isCancelable = false
        if (!completeFragment.isVisible) completeFragment.show(supportFragmentManager, CompleteFragment.tag)
    }

    private fun hideLoading() {
        val loadingDialog: LoadingDialogFragment =
            supportFragmentManager.findFragmentByTag(loadingDialogTag) as LoadingDialogFragment?
                ?: return
        if (!loadingDialog.isVisible || !loadingDialog.isAdded) return
        loadingDialog.dismissDialog()
    }

    override fun onBackPressed() {
        backStep()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        val fragment =
            supportFragmentManager.findFragmentByTag("DetailDeploymentSiteFragment") as DetailDeploymentSiteFragment?
                ?: DetailDeploymentSiteFragment.newInstance()
        fragment.onActivityResult(requestCode, resultCode, data)
    }

    override fun onDestroy() {
        super.onDestroy()

        if (::audioMothDeploymentViewModel.isInitialized) {
            audioMothDeploymentViewModel.onDestroy()
        }
    }

    companion object {
        const val loadingDialogTag = "LoadingDialog"
        const val EXTRA_DEPLOYMENT_ID = "EXTRA_DEPLOYMENT_ID"
        const val TONE_DURATION = 10000

        fun startActivity(context: Context) {
            val intent = Intent(context, AudioMothDeploymentActivity::class.java)
            context.startActivity(intent)
        }
    }
}
