package org.rfcx.audiomoth.view.deployment

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import io.realm.Realm
import kotlinx.android.synthetic.main.activity_deployment.*
import org.rfcx.audiomoth.BuildConfig
import org.rfcx.audiomoth.R
import org.rfcx.audiomoth.entity.*
import org.rfcx.audiomoth.localdb.DeploymentDb
import org.rfcx.audiomoth.localdb.DeploymentImageDb
import org.rfcx.audiomoth.localdb.LocateDb
import org.rfcx.audiomoth.localdb.ProfileDb
import org.rfcx.audiomoth.service.DeploymentSyncWorker
import org.rfcx.audiomoth.util.*
import org.rfcx.audiomoth.view.deployment.configure.ConfigureFragment
import org.rfcx.audiomoth.view.deployment.configure.SelectProfileFragment
import org.rfcx.audiomoth.view.deployment.guardian.GuardianDeploymentActivity
import org.rfcx.audiomoth.view.deployment.locate.LocationFragment
import org.rfcx.audiomoth.view.deployment.locate.MapPickerFragment
import org.rfcx.audiomoth.view.deployment.sync.SyncFragment
import org.rfcx.audiomoth.view.deployment.sync.SyncFragment.Companion.BEFORE_SYNC
import org.rfcx.audiomoth.view.deployment.verify.PerformBatteryFragment
import org.rfcx.audiomoth.view.deployment.verify.PerformBatteryFragment.Companion.TEST_BATTERY
import org.rfcx.audiomoth.view.dialog.CompleteFragment
import org.rfcx.audiomoth.view.dialog.CompleteListener
import org.rfcx.audiomoth.view.dialog.LoadingDialogFragment
import java.sql.Timestamp
import java.util.*

class DeploymentActivity : AppCompatActivity(), DeploymentProtocol, CompleteListener {
    // manager database
    private val realm by lazy { Realm.getInstance(RealmHelper.migrationConfig()) }
    private val deploymentDb by lazy { DeploymentDb(realm) }
    private val locateDb by lazy { LocateDb(realm) }
    private val profileDb by lazy { ProfileDb(realm) }
    private val deploymentImageDb by lazy { DeploymentImageDb(realm) }

    private var currentStep = 0
    private var _profiles: List<Profile> = listOf()
    private var _profile: Profile? = null
    private var _deployment: Deployment? = null
    private var _deployLocation: DeploymentLocation? = null
    private var _configuration: Configuration? = null
    private var _deploymentImages: List<DeploymentImage> = listOf()
    private var isReconfigure = false

    private val audioMothConnector: AudioMothConnector = AudioMothChimeConnector()
    private val configuration = AudioMothConfiguration()
    private val calendar = Calendar.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_deployment)
        initIntent()
    }

    private fun initIntent() {
        val deploymentId = intent.extras?.getInt(EXTRA_DEPLOYMENT_ID)
        isReconfigure = intent.extras?.getBoolean(EXTRA_RECONFIGURE) ?: false
        if (deploymentId != null) {
            handleDeploymentStep(deploymentId)
        } else {
            if (BuildConfig.ENABLE_GUARDIAN) {
                startFragment(ChooseDeviceFragment.newInstance())
            } else {
                openWithEdgeDevice()
            }
        }
    }

    override fun openWithEdgeDevice() {
        setupView()
    }

    override fun openWithGuardianDevice() {
        GuardianDeploymentActivity.startActivity(this)
        finish()
    }

    override fun hideCompleteButton() {
        completeStepButton.visibility = View.INVISIBLE
    }

    override fun showStepView() {
        stepView.visibility = View.VISIBLE
    }

    override fun hideStepView() {
        stepView.visibility = View.GONE
    }

    override fun showCompleteButton() {
        completeStepButton.visibility = View.VISIBLE
    }

    override fun setCompleteTextButton(text: String) {
        completeStepButton.text = text
    }

    override fun nextStep() {
        currentStep += 1

        if (stepView.stepCount == currentStep) {
            stepView.done(true)
            hideCompleteButton()
        } else {
            stepView.go(currentStep, true)
        }

        handleFragment(currentStep)
    }

    override fun backStep() {
        if (currentStep == 0) {
            finish()
        } else {
            currentStep = stepView.currentStep - 1
            stepView.go(currentStep, true)
            handleFragment(currentStep)
        }
    }

    override fun getDeployment(): Deployment? = this._deployment

    override fun setDeployment(deployment: Deployment) {
        this._deployment = deployment
    }

    override fun setDeploymentConfigure(profile: Profile) {
        setProfile(profile)
        this._configuration = profile.asConfiguration()
        this._deployment?.configuration = _configuration

        // update deployment
        _deployment?.let { deploymentDb.updateDeployment(it) }
        // update profile
        if (profile.name.isNotEmpty()) {
            if (!profileDb.isExistingProfile(profile.name)) {
                profileDb.insertOrUpdateProfile(profile)
            }
        }

        nextStep()
    }

    override fun geConfiguration(): Configuration? = _configuration

    override fun getDeploymentLocation(): DeploymentLocation? = this._deployLocation

    override fun setDeployLocation(locate: Locate) {
        val deployment = _deployment ?: Deployment()
        if (!isReconfigure) {
            deployment.state = DeploymentState.Edge.Locate.key // state
        }

        this._deployLocation = locate.asDeploymentLocation()
        val deploymentId = deploymentDb.insertOrUpdate(deployment, _deployLocation!!)
        locateDb.insertOrUpdateLocate(deploymentId, locate) // update locate - last deployment
        setDeployment(deployment)
    }

    override fun getProfiles(): List<Profile> = this._profiles

    override fun getProfile(): Profile? = this._profile

    override fun setProfile(profile: Profile) {
        this._profile = profile
    }

    override fun getDeploymentImage(): List<DeploymentImage> = this._deploymentImages

    override fun setPerformBattery(batteryDepletedAt: Timestamp, batteryLevel: Int) {
        this._deployment?.let {
            it.batteryDepletedAt = batteryDepletedAt
            it.batteryLevel = batteryLevel

            // update about battery
            this.deploymentDb.updateDeployment(it)
        }
    }

    override fun setReadyToDeploy(images: List<String>) {
        stepView.done(true)
        showLoading()
        _deployment?.let {
            it.deployedAt = Date()
            it.state = DeploymentState.Edge.ReadyToUpload.key
            setDeployment(it)

            deploymentImageDb.insertImage(it, images)
            deploymentDb.updateDeployment(it)

            DeploymentSyncWorker.enqueue(this@DeploymentActivity)
            showComplete()
        }
    }

    override fun startSetupConfigure(profile: Profile) {
        setProfile(profile)
        currentStep = 1
        stepView.go(currentStep, true)
        startFragment(ConfigureFragment.newInstance())
    }

    override fun startSyncing(status: String) {
        startFragment(SyncFragment.newInstance(status))
    }

    override fun startLocation(latitude: Double, longitude: Double) {
        startFragment(LocationFragment.newInstance(latitude, longitude))
    }

    override fun playSyncSound() {
        convertProfileToAudioMothConfiguration()
        Thread {
            audioMothConnector.setConfiguration(
                calendar,
                configuration,
                arrayOf(0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08)
            )
            this@DeploymentActivity.runOnUiThread {
                startSyncing(SyncFragment.AFTER_SYNC)
            }
        }.start()
    }

    private fun convertProfileToAudioMothConfiguration() {
        val deployment = _deployment
        if (deployment != null) {
            configuration.sampleRate = deployment.getSampleRate()
            configuration.gain = deployment.getGain()
            configuration.sleepRecordCycle = deployment.getSleepRecordCycle()
            configuration.startStopPeriods = deployment.getStartStopPeriods()
        }
    }

    override fun playCheckBatterySound() {
        Thread { audioMothConnector.getBatteryState() }.start()
    }

    override fun startCheckBattery(status: String, level: Int?) {
        startFragment(PerformBatteryFragment.newInstance(status, level))
    }

    override fun startMapPicker() {
        hideStepView()
        startFragment(MapPickerFragment.newInstance())
    }

    private fun setupView() {
        handleFragment(currentStep) // start page
        completeStepButton.setOnClickListener {
            nextStep()
        }
    }

    private fun handleDeploymentStep(deploymentId: Int) {
        val deployment = deploymentDb.getDeploymentById(deploymentId)
        if (deployment != null) {
            setDeployment(deployment)
            _deploymentImages = deploymentImageDb.getByDeploymentId(deploymentId)

            if (deployment.location != null) {
                _deployLocation = deployment.location
            }

            deployment.configuration?.let {
                _configuration = it
                val profile = it.toProfile()
                setProfile(profile) // on Config page need Profile
            }

            // is reconfigure? should be start edit first step
            currentStep = if (isReconfigure) 0 else deployment.state - 1
            stepView.go(currentStep, true)
            handleFragment(currentStep)
        }
    }

    private fun handleFragment(currentStep: Int) {
        // setup fragment for current step
        when (currentStep) {
            0 -> {
                updateDeploymentState(DeploymentState.Edge.Locate)
                startFragment(LocationFragment.newInstance())
            }
            1 -> {
                updateDeploymentState(DeploymentState.Edge.Config)
                handleSelectingConfig()
            }
            2 -> {
                updateDeploymentState(DeploymentState.Edge.Sync)
                startFragment(SyncFragment.newInstance(BEFORE_SYNC))
            }
            3 -> {
                updateDeploymentState(DeploymentState.Edge.Verify)
                startFragment(PerformBatteryFragment.newInstance(TEST_BATTERY, null))
            }
            4 -> {
                updateDeploymentState(DeploymentState.Edge.Deploy)
                startFragment(DeployFragment.newInstance())
            }
        }
    }

    private fun handleSelectingConfig() {
        if (_profile != null) {
            val profile = _profile
            if (profile != null) {
                startSetupConfigure(profile)
            }
        } else {
            this._profiles = profileDb.getProfiles()
            if (_profiles.isNotEmpty()) {
                startFragment(SelectProfileFragment.newInstance())
            } else {
                startSetupConfigure(Profile.default())
            }
        }
    }

    private fun startFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(contentContainer.id, fragment)
            .commit()
    }

    private fun updateDeploymentState(state: DeploymentState.Edge) {
        // isReconfigure don't update state
        if (!isReconfigure) {
            this._deployment?.state = state.key
        } else if (isReconfigure &&
            state == DeploymentState.Edge.ReadyToUpload &&
            _deployment?.syncState == SyncState.Sent.key
        ) {
            // is sent reset to unsent
            this._deployment?.syncState = SyncState.Unsent.key
        }
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
        completeFragment.show(supportFragmentManager, CompleteFragment.tag)
    }

    override fun onAnimationEnd() {
        finish()
    }

    override fun onBackPressed() {
        backStep()
    }

    companion object {
        const val loadingDialogTag = "LoadingDialog"
        private const val EXTRA_DEPLOYMENT_ID = "EXTRA_DEPLOYMENT_ID"
        private const val EXTRA_RECONFIGURE = "EXTRA_RECONFIGURE"

        fun startActivity(context: Context) {
            val intent = Intent(context, DeploymentActivity::class.java)
            context.startActivity(intent)
        }

        fun startActivity(context: Context, deploymentId: Int) {
            val intent = Intent(context, DeploymentActivity::class.java)
            intent.putExtra(EXTRA_DEPLOYMENT_ID, deploymentId)
            context.startActivity(intent)
        }

        fun startActivityForResult(
            context: Context, deploymentId: Int,
            isReconfigure: Boolean = false, requestCode: Int
        ) {
            val intent = Intent(context, DeploymentActivity::class.java)
            intent.putExtra(EXTRA_DEPLOYMENT_ID, deploymentId)
            intent.putExtra(EXTRA_RECONFIGURE, isReconfigure)
            (context as Activity).startActivityForResult(intent, requestCode)
        }
    }
}
