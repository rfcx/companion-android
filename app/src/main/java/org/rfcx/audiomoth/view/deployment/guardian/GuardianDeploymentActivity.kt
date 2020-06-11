package org.rfcx.audiomoth.view.deployment.guardian

import org.rfcx.audiomoth.view.deployment.DeployFragment
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import kotlinx.android.synthetic.main.activity_deployment.*
import org.rfcx.audiomoth.R
import org.rfcx.audiomoth.entity.*
import org.rfcx.audiomoth.view.LoadingDialogFragment
import org.rfcx.audiomoth.view.deployment.configure.ConfigureFragment
import org.rfcx.audiomoth.view.deployment.guardian.configure.GuardianConfigureFragment
import org.rfcx.audiomoth.view.deployment.guardian.configure.GuardianSelectProfileFragment
import org.rfcx.audiomoth.view.deployment.guardian.connect.ConnectGuardianFragment
import org.rfcx.audiomoth.view.deployment.guardian.deploy.GuardianDeployFragment
import org.rfcx.audiomoth.view.deployment.guardian.sync.GuardianSyncFragment
import org.rfcx.audiomoth.view.deployment.locate.LocationFragment
import org.rfcx.audiomoth.view.deployment.sync.SyncFragment
import org.rfcx.audiomoth.view.deployment.sync.SyncFragment.Companion.BEFORE_SYNC
import java.util.*

class GuardianDeploymentActivity : AppCompatActivity(), GuardianDeploymentProtocol {
    // manager database
    // TODO: need to implement db for guardian
//    private val realm by lazy { Realm.getInstance(RealmHelper.migrationConfig()) }
//    private val deploymentDb by lazy { DeploymentDb(realm) }
//    private val locateDb by lazy { LocateDb(realm) }
//    private val profileDb by lazy { ProfileDb(realm) }
//    private val deploymentImageDb by lazy { DeploymentImageDb(realm) }

    private var currentStep = 0
    private var _profiles: List<Profile> = listOf()
    private var _profile: Profile? = null
    private var _deployment: Deployment? = null
    private var _deployLocation: DeploymentLocation? = null
    private var _configuration: Configuration? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_guardian_deployment)
        val deploymentId = intent.extras?.getInt(DEPLOYMENT_ID)
        if (deploymentId != null) {
//            val deployment = deploymentDb.getDeploymentById(deploymentId)
//            if (deployment != null) {
//                setDeployment(deployment)
//
//                if (deployment.location != null) {
//                    _deployLocation = deployment.location
//                }
//
//                if (deployment.configuration != null) {
//                    _configuration = deployment.configuration
//                }
//                currentStep = deployment.state - 1
//                stepView.go(currentStep, true)
//                handleFragment(currentStep)
//            }
        } else {
            setupView()
        }
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
        stepView.go(stepView.currentStep - 1, true)
    }

//    override fun getDeployment(): Deployment? = this._deployment
//
//    override fun setDeployment(deployment: Deployment) {
//        this._deployment = deployment
//    }
//
    override fun setDeploymentConfigure() {
        //TODO: make it with guardian db
//        setProfile(profile)
//        this._configuration = profile.asConfiguration()
//        this._deployment?.configuration = _configuration

        // update deployment
//        _deployment?.let { deploymentDb.updateDeployment(it) }
        // update profile
//        if (profile.name.isNotEmpty()) {
//            profileDb.insertOrUpdateProfile(profile)
//        }

        nextStep()
    }

//    override fun geConfiguration(): Configuration? = _configuration
//
    override fun getDeploymentLocation(): DeploymentLocation? = this._deployLocation

    override fun setDeployLocation(locate: Locate) {
        val deployment = _deployment ?: Deployment()
        deployment.state = DeploymentState.Guardian.Locate.key // state

        this._deployLocation = locate.asDeploymentLocation()
//        val deploymentId = deploymentDb.insertOrUpdateDeployment(deployment, _deployLocation!!)
//        locateDb.insertOrUpdateLocate(deploymentId, locate) // update locate - last deployment

//        setDeployment(deployment)
    }

//    override fun getProfiles(): List<Profile> = this._profiles
//
//    override fun getProfile(): Profile? = this._profile
//
//    override fun setProfile(profile: Profile) {
//        this._profile = profile
//    }
//
//    override fun setPerformBattery(batteryDepletedAt: Timestamp, batteryLevel: Int) {
//        this._deployment?.let {
//            it.batteryDepletedAt = batteryDepletedAt
//            it.batteryLevel = batteryLevel
//
//            // update about battery
////            this.deploymentDb.updateDeployment(it)
//        }
//    }

    override fun setReadyToDeploy(images: List<String>) {
        stepView.done(true)
        showLoading()
        _deployment?.let {
            it.deployedAt = Date()
            it.state = DeploymentState.Guardian.ReadyToUpload.key
//            setDeployment(it)

//            deploymentImageDb.insertImage(it, images)
//            deploymentDb.updateDeployment(it)
            saveDevelopment(it)
        }
    }

    override fun startSetupConfigure() {
        currentStep = 2
        stepView.go(currentStep, true)
        startFragment(GuardianConfigureFragment.newInstance())
    }

    override fun startSyncing(status: String) {
        startFragment(SyncFragment.newInstance(status))
    }

    private fun handleFragment(currentStep: Int) {
        // setup fragment for current step
        when (currentStep) {
            0 -> {
                updateDeploymentState(DeploymentState.Guardian.Connect)
                startFragment(ConnectGuardianFragment.newInstance())
            }
            1 -> {
                updateDeploymentState(DeploymentState.Guardian.Locate)
                startFragment(LocationFragment.newInstance())
            }
            2 -> {
                updateDeploymentState(DeploymentState.Guardian.Config)
                startFragment(GuardianSelectProfileFragment.newInstance())
            }
            3 -> {
                updateDeploymentState(DeploymentState.Guardian.Sync)
                startFragment(GuardianSyncFragment.newInstance(BEFORE_SYNC))
            }
            4 -> {
                updateDeploymentState(DeploymentState.Guardian.Deploy)
                startFragment(GuardianDeployFragment.newInstance())
            }
        }
    }

//    private fun handleSelectingConfig() {
//        if (_configuration != null) {
//            val config = _configuration
//            if (config != null) {
//                val profile = Profile(
//                    gain = config.gain,
//                    name = "",
//                    sampleRate = config.sampleRate,
//                    recordingDuration = config.recordingDuration,
//                    sleepDuration = config.sleepDuration,
//                    recordingPeriodList = config.recordingPeriodList,
//                    durationSelected = config.durationSelected
//                )
//                startSetupConfigure(profile)
//            }
//        } else {
////            this._profiles = profileDb.getProfiles()
//            if (_profiles.isNotEmpty()) {
//                startFragment(SelectProfileFragment.newInstance())
//            } else {
//                startSetupConfigure(Profile.default())
//            }
//        }
//    }

    private fun startFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(contentContainer.id, fragment)
            .commit()
    }

    private fun updateDeploymentState(state: DeploymentState.Guardian) {
        this._deployment?.state = state.key
//        this._deployment?.let { deploymentDb.updateDeployment(it) }
    }

    private fun saveDevelopment(deployment: Deployment) {
//        Firestore(this).saveDeployment(deploymentDb, deployment) { string, isSuccess ->
//            if (isSuccess) {
//                Toast.makeText(
//                    this,
//                    getString(R.string.deployment_uploaded),
//                    Toast.LENGTH_SHORT
//                ).show()
//                hideLoading()
//                finish()
//            } else {
//                hideLoading()
//                showCommonDialog(
//                    title = "",
//                    message = string ?: getString(R.string.error_upload_deployment),
//                    onClick = DialogInterface.OnClickListener { dialog, _ ->
//                        dialog.dismiss()
//                        finish()
//                    }
//                )
//            }
//        }
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

    companion object {
        const val loadingDialogTag = "LoadingDialog"
        const val DEPLOYMENT_ID = "DEPLOYMENT_ID"

        fun startActivity(context: Context) {
            val intent = Intent(context, GuardianDeploymentActivity::class.java)
            context.startActivity(intent)
        }

        fun startActivity(context: Context, deploymentId: Int) {
            val intent = Intent(context, GuardianDeploymentActivity::class.java)
            intent.putExtra(DEPLOYMENT_ID, deploymentId)
            context.startActivity(intent)
        }
    }
}
