package org.rfcx.companion.view.deployment

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.location.Location
import android.location.LocationManager
import android.os.Bundle
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import io.realm.RealmList
import kotlinx.android.synthetic.main.activity_deployment.*
import kotlinx.android.synthetic.main.toolbar_default.*
import org.rfcx.companion.R
import org.rfcx.companion.base.ViewModelFactory
import org.rfcx.companion.entity.*
import org.rfcx.companion.entity.guardian.Deployment
import org.rfcx.companion.localdb.*
import org.rfcx.companion.repo.api.DeviceApiHelper
import org.rfcx.companion.repo.api.DeviceApiServiceImpl
import org.rfcx.companion.repo.local.LocalDataHelper
import org.rfcx.companion.service.DeploymentSyncWorker
import org.rfcx.companion.util.*
import org.rfcx.companion.util.Preferences.Companion.ENABLE_LOCATION_TRACKING
import org.rfcx.companion.util.geojson.GeoJsonUtils
import org.rfcx.companion.view.deployment.guardian.GuardianDeploymentActivity
import org.rfcx.companion.view.deployment.locate.MapPickerFragment
import org.rfcx.companion.view.deployment.location.DetailDeploymentSiteFragment
import org.rfcx.companion.view.deployment.location.SetDeploymentSiteFragment
import org.rfcx.companion.view.deployment.sync.NewSyncFragment
import org.rfcx.companion.view.dialog.*
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
    private var sites = listOf<Locate>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_deployment)

        setupToolbar()
        setViewModel()
        setObserver()
        preferences.clearSelectedProject()
        this.currentLocate = this.getLastLocation()
        val deploymentId = intent.extras?.getInt(EXTRA_DEPLOYMENT_ID)
        if (deploymentId != null) {
            handleDeploymentStep(deploymentId)
        } else {
            openWithEdgeDevice()
        }
    }

    private fun setViewModel() {
        audioMothDeploymentViewModel = ViewModelProvider(
            this,
            ViewModelFactory(
                application,
                DeviceApiHelper(DeviceApiServiceImpl()),
                LocalDataHelper()
            )
        ).get(AudioMothDeploymentViewModel::class.java)
    }

    private fun setObserver() {
        audioMothDeploymentViewModel.getDeployments().observe(this, Observer {
            this.deployments = it.filter { deployment -> deployment.isCompleted() }
            setSiteItems()
        })

        audioMothDeploymentViewModel.getSites().observe(this, Observer {
            this.sites = it
            setSiteItems()
        })
    }

    private fun setSiteItems() {
        val loc = Location(LocationManager.GPS_PROVIDER)
        loc.latitude = 0.0
        loc.longitude = 0.0

        _siteItems = getListSite(
            this,
            deployments,
            getString(R.string.none),
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
        audioMothDeploymentViewModel.insertImage(deployment, _images)
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
            is MapPickerFragment -> startFragment(
                DetailDeploymentSiteFragment.newInstance(
                    latitude,
                    longitude,
                    siteId,
                    nameLocation
                )
            )
            is AudioMothCheckListFragment -> {
                _deployment?.let {
                    it.passedChecks = passedChecks
                    audioMothDeploymentViewModel.updateDeployment(it)

                    saveImages(it)
                }
                passedChecks.clear() // remove all passed
                finish()
            }
            is DetailDeploymentSiteFragment -> {
                if (_deployLocation == null) {
                    startFragment(
                        SetDeploymentSiteFragment.newInstance(
                            currentLocate?.latitude ?: 0.0, currentLocate?.longitude ?: 0.0
                        )
                    )
                } else {
                    startCheckList()
                }
            }
            is ChooseDeviceFragment -> finish()
            else -> startCheckList()
        }
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

    override fun getLocationGroup(name: String): Project? {
        return audioMothDeploymentViewModel.getProjectByName(name)
    }

    override fun setDeployment(deployment: Deployment) {
        this._deployment = deployment
    }

    override fun setDeployLocation(locate: Locate, isExisted: Boolean) {
        val deployment = _deployment ?: Deployment()
        deployment.device = Device.AUDIOMOTH.value
        deployment.isActive = locate.serverId == null
        deployment.state = DeploymentState.AudioMoth.Locate.key // state

        this._deployLocation = locate.asDeploymentLocation()
        this._locate = locate
        useExistedLocation = isExisted
        if (!useExistedLocation) {
            audioMothDeploymentViewModel.insertOrUpdate(locate)
        }

        setDeployment(deployment)
    }

    override fun setReadyToDeploy() {
        showLoading()
        _deployment?.let {
            it.deployedAt = Date()
            it.updatedAt = Date()
            it.isActive = true
            it.state = DeploymentState.AudioMoth.ReadyToUpload.key
            setDeployment(it)

            val deploymentId =
                audioMothDeploymentViewModel.insertOrUpdateDeployment(it, _deployLocation!!)
            this._locate?.let { loc ->
                audioMothDeploymentViewModel.insertOrUpdateLocate(
                    deploymentId,
                    loc
                ) // update locate - last deployment
            }

            if (useExistedLocation) {
                this._locate?.let { locate ->
                    val deployments =
                        locate.serverId?.let { it1 ->
                            audioMothDeploymentViewModel.getDeploymentsBySiteId(
                                it1
                            )
                        }
                    deployments?.forEach { deployment ->
                        audioMothDeploymentViewModel.updateIsActive(deployment.id)
                    }
                }
            }
            saveImages(it)

            //track getting
            if (preferences.getBoolean(ENABLE_LOCATION_TRACKING)) {
                val track = audioMothDeploymentViewModel.getFirstTracking()
                track?.let { t ->
                    val point = t.points.toListDoubleArray()
                    val trackingFile = TrackingFile(
                        deploymentId = it.id,
                        siteId = this._locate!!.id,
                        localPath = GeoJsonUtils.generateGeoJson(
                            this,
                            GeoJsonUtils.generateFileName(it.deployedAt, it.deploymentKey!!),
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
                val site = this._locate
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
                updateDeploymentState(DeploymentState.AudioMoth.Sync)
                startFragment(NewSyncFragment.newInstance())
            }
            2 -> {
                updateDeploymentState(DeploymentState.AudioMoth.Deploy)
                startFragment(DeployFragment.newInstance())
            }
        }
    }

    override fun getPassedChecks(): List<Int> = passedChecks

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
                    supportFragmentManager.findFragmentById(R.id.contentContainer) as NewSyncFragment
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

    private fun handleDeploymentStep(deploymentId: Int) {
        val deployment = audioMothDeploymentViewModel.getDeploymentById(deploymentId)
        if (deployment != null) {
            setDeployment(deployment)

            if (deployment.stream != null) {
                _deployLocation = deployment.stream
            }

            if (deployment.passedChecks != null) {
                val passedChecks = deployment.passedChecks
                this.passedChecks = passedChecks ?: RealmList<Int>()
            }

            val images = audioMothDeploymentViewModel.getImageByDeploymentId(deployment.id)
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

        if (::audioMothDeploymentViewModel.isInitialized) {
            audioMothDeploymentViewModel.onDestroy()
        }
    }

    companion object {
        const val loadingDialogTag = "LoadingDialog"
        const val EXTRA_DEPLOYMENT_ID = "EXTRA_DEPLOYMENT_ID"
        const val TONE_DURATION = 10000

        private var fromUnfinishedDeployment = false

        fun startActivity(context: Context) {
            val intent = Intent(context, AudioMothDeploymentActivity::class.java)
            context.startActivity(intent)
        }

        fun startActivity(context: Context, deploymentId: Int) {
            val intent = Intent(context, AudioMothDeploymentActivity::class.java)
            intent.putExtra(EXTRA_DEPLOYMENT_ID, deploymentId)
            context.startActivity(intent)
        }

        fun startActivity(context: Context, deploymentId: Int, requestCode: Int) {
            val intent = Intent(context, AudioMothDeploymentActivity::class.java)
            intent.putExtra(EXTRA_DEPLOYMENT_ID, deploymentId)
            fromUnfinishedDeployment = true
            (context as Activity).startActivityForResult(intent, requestCode)
        }
    }
}
