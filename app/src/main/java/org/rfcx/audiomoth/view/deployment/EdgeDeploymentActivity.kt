package org.rfcx.audiomoth.view.deployment

import android.app.AlarmManager
import android.app.PendingIntent
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.recyclerview.widget.LinearLayoutManager
import io.realm.Realm
import kotlinx.android.synthetic.main.activity_deployment.*
import org.rfcx.audiomoth.BuildConfig
import org.rfcx.audiomoth.R
import org.rfcx.audiomoth.entity.*
import org.rfcx.audiomoth.localdb.DeploymentImageDb
import org.rfcx.audiomoth.localdb.EdgeDeploymentDb
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
import org.rfcx.audiomoth.view.detail.MapPickerProtocol
import org.rfcx.audiomoth.view.dialog.CompleteFragment
import org.rfcx.audiomoth.view.dialog.CompleteListener
import org.rfcx.audiomoth.view.dialog.LoadingDialogFragment
import java.sql.Timestamp
import java.util.*

class EdgeDeploymentActivity : AppCompatActivity(), EdgeDeploymentProtocol, CompleteListener,
    MapPickerProtocol, (Int) -> Unit {
    // manager database
    private val realm by lazy { Realm.getInstance(RealmHelper.migrationConfig()) }
    private val deploymentDb by lazy {
        EdgeDeploymentDb(
            realm
        )
    }
    private val locateDb by lazy { LocateDb(realm) }
    private val profileDb by lazy { ProfileDb(realm) }
    private val deploymentImageDb by lazy { DeploymentImageDb(realm) }

    private val edgeStepView by lazy { StepViewAdapter(this) }

    private var currentStep = 0
    private var _profiles: List<Profile> = listOf()
    private var _profile: Profile? = null
    private var _deployment: EdgeDeployment? = null
    private var _deployLocation: DeploymentLocation? = null
    private var _edgeConfiguration: EdgeConfiguration? = null

    private val audioMothConnector: AudioMothConnector = AudioMothChimeConnector()
    private val configuration = AudioMothConfiguration()
    private val calendar = Calendar.getInstance()

    private var latitude = 0.0
    private var longitude = 0.0
    private var nameLocation: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_deployment)

        setupStepView()

        val deploymentId = intent.extras?.getInt(EXTRA_DEPLOYMENT_ID)
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

    private fun setupStepView() {
        edgeStepRecyclerView.apply {
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
            adapter = edgeStepView
        }
        edgeStepView.setSteps(this.resources.getStringArray(R.array.audiomoth_steps).toList())
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
        edgeStepRecyclerView.visibility = View.VISIBLE
    }

    override fun hideStepView() {
        edgeStepRecyclerView.visibility = View.GONE
    }

    override fun showCompleteButton() {
        completeStepButton.visibility = View.VISIBLE
    }

    override fun setCompleteTextButton(text: String) {
        completeStepButton.text = text
    }

    override fun invoke(number: Int) {
        currentStep = number - 1
        handleFragment(currentStep)
    }

    override fun nextStep() {
        currentStep += 1
        handleFragment(currentStep)
    }

    override fun backStep() {
        if (currentStep == 0) {
            finish()
        } else {
            currentStep -= 1
            handleFragment(currentStep)
        }
    }

    override fun getDeployment(): EdgeDeployment? = this._deployment

    override fun setDeployment(deployment: EdgeDeployment) {
        this._deployment = deployment
    }

    override fun setDeploymentConfigure(profile: Profile) {
        setProfile(profile)
        this._edgeConfiguration = profile.asConfiguration()
        this._deployment?.configuration = _edgeConfiguration

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

    override fun geConfiguration(): EdgeConfiguration? =
        _edgeConfiguration

    override fun getDeploymentLocation(): DeploymentLocation? = this._deployLocation

    override fun setDeployLocation(locate: Locate) {
        val deployment = _deployment ?: EdgeDeployment()
        deployment.state = DeploymentState.Edge.Locate.key // state
        deployment.deploymentId = randomDeploymentId()

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

    private fun setLatLng(latitude: Double, longitude: Double, name: String) {
        this.latitude = latitude
        this.longitude = longitude
        this.nameLocation = name
    }

    override fun setPerformBattery(batteryDepletedAt: Timestamp, batteryLevel: Int) {
        this._deployment?.let {
            it.batteryDepletedAt = batteryDepletedAt
            it.batteryLevel = batteryLevel

            // update about battery
            this.deploymentDb.updateDeployment(it)
        }
    }

    override fun setReadyToDeploy(images: List<String>) {
        showLoading()
        _deployment?.let {
            it.deployedAt = Date()
            it.updatedAt = Date()
            it.state = DeploymentState.Edge.ReadyToUpload.key
            setDeployment(it)

            deploymentImageDb.insertImage(it, images)
            deploymentDb.updateDeployment(it)

            DeploymentSyncWorker.enqueue(this@EdgeDeploymentActivity)
            notification()
            hideLoading()
            showComplete()
        }
    }

    override fun startSetupConfigure(profile: Profile) {
        setProfile(profile)
        currentStep = 1
        startFragment(ConfigureFragment.newInstance())
    }

    override fun startSyncing(status: String) {
        startFragment(SyncFragment.newInstance(status))
    }

    override fun startLocationPage(latitude: Double, longitude: Double, name: String) {
        startFragment(LocationFragment.newInstance(latitude, longitude, name))
    }

    override fun playSyncSound() {
        val deploymentId = getDeployment()?.deploymentId
        convertProfileToAudioMothConfiguration()
        Thread {
            audioMothConnector.setConfiguration(
                calendar,
                configuration,
                deploymentId?.let { DeploymentIdentifier(it) }
            )
            this@EdgeDeploymentActivity.runOnUiThread {
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

    override fun startMapPicker(latitude: Double, longitude: Double, name: String) {
        hideStepView()
        setLatLng(latitude, longitude, name)
        startFragment(MapPickerFragment.newInstance(latitude, longitude, name))
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

            if (deployment.location != null) {
                _deployLocation = deployment.location
            }

            if (deployment.configuration != null) {
                _edgeConfiguration = deployment.configuration
            }
            currentStep = if (deployment.state == 1) {
                deployment.state
            } else {
                deployment.state - 1
            }
            handleFragment(currentStep)
        }
    }

    private fun handleFragment(currentStep: Int) {
        // setup fragment for current step
        handleStepView(currentStep)
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

    private fun handleStepView(currentStep: Int) {
        edgeStepView.setStepPasses(currentStep)
        edgeStepRecyclerView.smoothScrollToPosition(currentStep * 2)
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
        finish()
    }

    override fun onBackPressed() {
        if (currentStep == 0) {
            val isFragmentPopped = handleNestedFragmentBackStack(supportFragmentManager)
            if (!isFragmentPopped && supportFragmentManager.backStackEntryCount <= 1) {
                // if top's fragment is  LocationFragment then finish else show LocationFragment fragment
                if (supportFragmentManager.fragments.firstOrNull() is LocationFragment) {
                    finish()
                } else {
                    startLocationPage(this.latitude, this.longitude, this.nameLocation)
                }
            } else if (!isFragmentPopped) {
                super.onBackPressed()
            }
        } else {
            backStep()
        }
    }

    private fun notification() {
        val edgeDeploymentId = _deployment?.deploymentId
        val day = 24 * 60 * 60 * 1000
        val intent = Intent(this, NotificationBroadcastReceiver::class.java)
        val dateAlarm = (_deployment?.batteryDepletedAt?.time)?.minus(day)?.let { Date(it) }

        intent.putExtra(EXTRA_BATTERY_DEPLETED_AT, _deployment?.batteryDepletedAt?.toDateTimeString())
        intent.putExtra(EXTRA_LOCATION_NAME, _deployment?.location?.name)
        intent.putExtra(EXTRA_DEPLOYMENT_ID, edgeDeploymentId)

        val pendingIntent =
            PendingIntent.getBroadcast(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)

        val cal = Calendar.getInstance()
        if (dateAlarm != null) {
            cal.time = dateAlarm
            if (dateAlarm.time > System.currentTimeMillis()) {
                val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
                alarmManager.setExact(
                    AlarmManager.RTC_WAKEUP,
                    cal.timeInMillis,
                    pendingIntent
                )
            }
        }
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

    companion object {
        const val loadingDialogTag = "LoadingDialog"
        const val EXTRA_DEPLOYMENT_ID = "EXTRA_DEPLOYMENT_ID"
        const val EXTRA_BATTERY_DEPLETED_AT = "EXTRA_BATTERY_DEPLETED_AT"
        const val EXTRA_LOCATION_NAME = "EXTRA_LOCATION_NAME"

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
            (context as Activity).startActivityForResult(intent, requestCode)
        }
    }
}
