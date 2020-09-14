package org.rfcx.audiomoth.view.deployment.guardian

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import io.realm.Realm
import kotlinx.android.synthetic.main.activity_guardian_deployment.*
import org.rfcx.audiomoth.R
import org.rfcx.audiomoth.connection.socket.SocketManager
import org.rfcx.audiomoth.entity.DeploymentLocation
import org.rfcx.audiomoth.entity.DeploymentState
import org.rfcx.audiomoth.entity.Locate
import org.rfcx.audiomoth.entity.guardian.GuardianConfiguration
import org.rfcx.audiomoth.entity.guardian.GuardianDeployment
import org.rfcx.audiomoth.entity.guardian.GuardianProfile
import org.rfcx.audiomoth.localdb.LocateDb
import org.rfcx.audiomoth.localdb.guardian.GuardianDeploymentDb
import org.rfcx.audiomoth.localdb.guardian.GuardianDeploymentImageDb
import org.rfcx.audiomoth.localdb.guardian.GuardianProfileDb
import org.rfcx.audiomoth.service.GuardianDeploymentSyncWorker
import org.rfcx.audiomoth.util.RealmHelper
import org.rfcx.audiomoth.view.deployment.StepViewAdapter
import org.rfcx.audiomoth.view.deployment.guardian.checkin.GuardianCheckInTestFragment
import org.rfcx.audiomoth.view.deployment.guardian.configure.GuardianConfigureFragment
import org.rfcx.audiomoth.view.deployment.guardian.configure.GuardianSelectProfileFragment
import org.rfcx.audiomoth.view.deployment.guardian.connect.ConnectGuardianFragment
import org.rfcx.audiomoth.view.deployment.guardian.deploy.GuardianDeployFragment
import org.rfcx.audiomoth.view.deployment.guardian.microphone.GuardianMicrophoneFragment
import org.rfcx.audiomoth.view.deployment.guardian.register.GuardianRegisterFragment
import org.rfcx.audiomoth.view.deployment.guardian.signal.GuardianSignalFragment
import org.rfcx.audiomoth.view.deployment.guardian.solarpanel.GuardianSolarPanelFragment
import org.rfcx.audiomoth.view.deployment.locate.LocationFragment
import org.rfcx.audiomoth.view.deployment.locate.MapPickerFragment
import org.rfcx.audiomoth.view.detail.MapPickerProtocol
import org.rfcx.audiomoth.view.dialog.CompleteFragment
import org.rfcx.audiomoth.view.dialog.CompleteListener
import org.rfcx.audiomoth.view.dialog.LoadingDialogFragment
import org.rfcx.audiomoth.view.prefs.SyncPreferenceListener
import java.util.*

class GuardianDeploymentActivity : AppCompatActivity(), GuardianDeploymentProtocol,
    CompleteListener, MapPickerProtocol, SyncPreferenceListener, (Int) -> Unit {
    // manager database
    private val realm by lazy { Realm.getInstance(RealmHelper.migrationConfig()) }
    private val locateDb by lazy { LocateDb(realm) }
    private val profileDb by lazy { GuardianProfileDb(realm) }
    private val deploymentDb by lazy { GuardianDeploymentDb(realm) }
    private val deploymentImageDb by lazy { GuardianDeploymentImageDb(realm) }

    private val guardianStepView by lazy { StepViewAdapter(this) }

    private var currentStep = 0
    private var _profiles: List<GuardianProfile> = listOf()
    private var _profile: GuardianProfile? = null
    private var _deployment: GuardianDeployment? = null
    private var _deployLocation: DeploymentLocation? = null
    private var _configuration: GuardianConfiguration? = null

    private var _sampleRate = 24000

    private var latitude = 0.0
    private var longitude = 0.0

    private var beforeStep = 0

    private var prefsChanges = mapOf<String, String>()
    private var prefsEditor: SharedPreferences.Editor? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_guardian_deployment)

        setupStepView()

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
                currentStep = deployment.state - 1
                handleFragment(currentStep)
            }
        } else {
            setupView()
        }
    }

    private fun setupStepView() {
        guardianStepRecyclerView.apply {
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
            adapter = guardianStepView
        }
        guardianStepView.setSteps(this.resources.getStringArray(R.array.guardian_steps).toList())
        guardianStepView.setStepsCanSkip(this.resources.getStringArray(R.array.guardian_can_skip_steps).toList())
    }

    private fun setupView() {
        handleFragment(currentStep) // start page
        completeStepButton.setOnClickListener {
            nextStep()
        }
    }

    override fun hideCompleteButton() {
        completeStepButton.visibility = View.INVISIBLE
    }

    override fun showCompleteButton() {
        completeStepButton.visibility = View.VISIBLE
    }

    override fun setCompleteTextButton(text: String) {
        completeStepButton.text = text
    }

    override fun invoke(number: Int) {
        beforeStep = currentStep
        currentStep = number - 1
        handleFragment(currentStep)
    }

    override fun nextStep() {
        beforeStep = currentStep
        guardianStepView.setStepPasses(currentStep)
        currentStep += 1
        handleFragment(currentStep)
    }

    override fun backStep() {
        beforeStep = currentStep
        when (currentStep) {
            0 -> finish()
            3 -> {
                val container = supportFragmentManager.findFragmentById(R.id.contentContainer)
                if (container is GuardianConfigureFragment) {
                    startFragment(GuardianSelectProfileFragment.newInstance())
                } else {
                    currentStep -= 1
                    handleFragment(currentStep)
                }
            }
            else -> {
                currentStep -= 1
                handleFragment(currentStep)
            }
        }
    }

    override fun hideStepView() {
    }

    override fun showStepView() {
    }

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

    override fun canDeploy(): Boolean {
        return guardianStepView.isEveryStepsPassed()
    }

    override fun setDeploymentConfigure(profile: GuardianProfile) {
        setProfile(profile)
        this._configuration = profile.asConfiguration()
        this._deployment?.configuration = _configuration

        // update deployment
        _deployment?.let { deploymentDb.updateDeployment(it) }
        // update profile
        if (profile.name.isNotEmpty()) {
            profileDb.insertOrUpdateProfile(profile)
        }
    }

    override fun getConfiguration(): GuardianConfiguration? = _configuration

    override fun getSampleRate(): Int = _sampleRate

    override fun getDeploymentLocation(): DeploymentLocation? = this._deployLocation

    override fun setDeployLocation(locate: Locate) {
        val deployment = _deployment ?: GuardianDeployment()
        deployment.state = DeploymentState.Guardian.Locate.key // state

        this._deployLocation = locate.asDeploymentLocation()
        val deploymentId = deploymentDb.insertOrUpdateDeployment(deployment, _deployLocation!!)
        locateDb.insertOrUpdateLocate(deploymentId, locate, true) // update locate - last deployment

        setDeployment(deployment)
    }

    override fun setReadyToDeploy(images: List<String>) {
        showLoading()
        _deployment?.let {
            it.deployedAt = Date()
            it.state = DeploymentState.Guardian.ReadyToUpload.key
            setDeployment(it)

            deploymentImageDb.insertImage(it, images)
            deploymentDb.updateDeployment(it)

            GuardianDeploymentSyncWorker.enqueue(this@GuardianDeploymentActivity)
            showComplete()
        }
    }

    override fun startSetupConfigure(profile: GuardianProfile) {
        setProfile(profile)
        currentStep = 3
        startFragment(GuardianConfigureFragment.newInstance())
    }

    override fun backToConfigure() {
        currentStep = 3
        startFragment(GuardianConfigureFragment.newInstance())
    }

    private fun handleFragment(currentStep: Int) {
        // setup fragment for current step
        handleStepView(currentStep)
        when (currentStep) {
            0 -> {
                updateDeploymentState(DeploymentState.Guardian.Connect)
                startFragment(ConnectGuardianFragment.newInstance())
            }
            1 -> {
                updateDeploymentState(DeploymentState.Guardian.Register)
                startFragment(GuardianRegisterFragment.newInstance())
            }
            2 -> {
                updateDeploymentState(DeploymentState.Guardian.Locate)
                startFragment(GuardianSignalFragment.newInstance())
            }
            3 -> {
                this._profiles = profileDb.getProfiles()
                updateDeploymentState(DeploymentState.Guardian.Config)
                startFragment(GuardianSelectProfileFragment.newInstance())
            }
            4 -> {
                updateDeploymentState(DeploymentState.Guardian.SolarPanel)
                startFragment(GuardianMicrophoneFragment.newInstance())
            }
            5 -> {
                updateDeploymentState(DeploymentState.Guardian.Signal)
                startFragment(GuardianSolarPanelFragment.newInstance())
            }
            6 -> {
                updateDeploymentState(DeploymentState.Guardian.Microphone)
                startFragment(LocationFragment.newInstance())
            }
            7 -> {
                updateDeploymentState(DeploymentState.Guardian.Checkin)
                startFragment(GuardianCheckInTestFragment.newInstance())
            }
            8 -> {
                updateDeploymentState(DeploymentState.Guardian.Deploy)
                startFragment(GuardianDeployFragment.newInstance())
            }
        }
    }

    private fun handleStepView(currentStep: Int) {
        guardianStepView.setStepUnSelected(beforeStep)
        guardianStepView.setStepSelected(currentStep)
        guardianStepRecyclerView.smoothScrollToPosition(currentStep * 2)
    }

    private fun startFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(contentContainer.id, fragment)
            .commit()
    }

    private fun updateDeploymentState(state: DeploymentState.Guardian) {
        this._deployment?.state = state.key
        this._deployment?.let { deploymentDb.updateDeployment(it) }
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

    override fun startMapPicker(latitude: Double, longitude: Double, name: String) {
        hideStepView()
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
    }

    companion object {
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

    override fun showSyncButton() { /* not used */ }

    override fun hideSyncButton() { /* not used */ }

    override fun syncPrefs() {/* not used */}

    override fun showSuccessResponse() { /* not used */ }

    override fun showFailedResponse() { /* not used */ }

    override fun setEditor(editor: SharedPreferences.Editor) {
        this.prefsEditor = editor
    }
}
