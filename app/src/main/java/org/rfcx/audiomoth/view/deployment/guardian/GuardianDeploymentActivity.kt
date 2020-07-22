package org.rfcx.audiomoth.view.deployment.guardian

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import io.realm.Realm
import kotlinx.android.synthetic.main.activity_deployment.*
import org.rfcx.audiomoth.R
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
import org.rfcx.audiomoth.view.deployment.guardian.configure.GuardianConfigureFragment
import org.rfcx.audiomoth.view.deployment.guardian.configure.GuardianSelectProfileFragment
import org.rfcx.audiomoth.view.deployment.guardian.connect.ConnectGuardianFragment
import org.rfcx.audiomoth.view.deployment.guardian.deploy.GuardianDeployFragment
import org.rfcx.audiomoth.view.deployment.guardian.verify.GuardianVerifyFragment
import org.rfcx.audiomoth.view.deployment.locate.LocationFragment
import org.rfcx.audiomoth.view.deployment.locate.MapPickerFragment
import org.rfcx.audiomoth.view.dialog.CompleteFragment
import org.rfcx.audiomoth.view.dialog.CompleteListener
import org.rfcx.audiomoth.view.dialog.LoadingDialogFragment
import java.util.*

class GuardianDeploymentActivity : AppCompatActivity(), GuardianDeploymentProtocol,
    CompleteListener {
    // manager database
    private val realm by lazy { Realm.getInstance(RealmHelper.migrationConfig()) }
    private val locateDb by lazy { LocateDb(realm) }
    private val profileDb by lazy { GuardianProfileDb(realm) }
    private val deploymentDb by lazy { GuardianDeploymentDb(realm) }
    private val deploymentImageDb by lazy { GuardianDeploymentImageDb(realm) }

    private var currentStep = 0
    private var _profiles: List<GuardianProfile> = listOf()
    private var _profile: GuardianProfile? = null
    private var _deployment: GuardianDeployment? = null
    private var _deployLocation: DeploymentLocation? = null
    private var _configuration: GuardianConfiguration? = null

    private var latitude = 0.0
    private var longitude = 0.0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_guardian_deployment)
        val deploymentId = intent.extras?.getInt(DEPLOYMENT_ID)
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
                stepView.go(currentStep, true)
                handleFragment(currentStep)
            }
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
        when (currentStep) {
            0 -> finish()
            2 -> {
                val container = supportFragmentManager.findFragmentById(R.id.contentContainer)
                if (container is GuardianConfigureFragment) {
                    startFragment(GuardianSelectProfileFragment.newInstance())
                } else {
                    currentStep = stepView.currentStep - 1
                    stepView.go(currentStep, true)
                    handleFragment(currentStep)
                }
            }
            else -> {
                currentStep = stepView.currentStep - 1
                stepView.go(currentStep, true)
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

    override fun setDeploymentConfigure(profile: GuardianProfile) {
        setProfile(profile)
        this._configuration = profile.asConfiguration()
        this._deployment?.configuration = _configuration

//         update deployment
        _deployment?.let { deploymentDb.updateDeployment(it) }
//         update profile
        if (profile.name.isNotEmpty()) {
            profileDb.insertOrUpdateProfile(profile)
        }
    }

    override fun getConfiguration(): GuardianConfiguration? = _configuration

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
        stepView.done(true)
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
        currentStep = 2
        stepView.go(currentStep, true)
        startFragment(GuardianConfigureFragment.newInstance())
    }

    override fun backToConfigure() {
        currentStep = 2
        stepView.go(currentStep, true)
        startFragment(GuardianConfigureFragment.newInstance())
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
                this._profiles = profileDb.getProfiles()
                updateDeploymentState(DeploymentState.Guardian.Config)
                startFragment(GuardianSelectProfileFragment.newInstance())
            }
            3 -> {
                updateDeploymentState(DeploymentState.Guardian.Sync)
                startFragment(GuardianVerifyFragment.newInstance())
            }
            4 -> {
                updateDeploymentState(DeploymentState.Guardian.Deploy)
                startFragment(GuardianDeployFragment.newInstance())
            }
        }
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

    override fun hideLoading() {
        val loadingDialog: LoadingDialogFragment? =
            supportFragmentManager.findFragmentByTag(loadingDialogTag) as LoadingDialogFragment?
        loadingDialog?.dismissDialog()
    }

    override fun startMapPicker(latitude: Double, longitude: Double) {
        hideStepView()
        setLatLng(latitude, longitude)
        startFragment(MapPickerFragment.newInstance(latitude, longitude))
    }

    private fun setLatLng(latitude: Double, longitude: Double) {
        this.latitude = latitude
        this.longitude = longitude
    }

    override fun startLocation(latitude: Double, longitude: Double) {
        startFragment(LocationFragment.newInstance(latitude, longitude))
    }

    override fun onBackPressed() {
        backStep()
    }

    override fun onAnimationEnd() {
        finish()
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
