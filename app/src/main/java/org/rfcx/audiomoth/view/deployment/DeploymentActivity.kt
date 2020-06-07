package org.rfcx.audiomoth.view.deployment

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.inputmethod.InputMethodManager
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import io.realm.Realm
import kotlinx.android.synthetic.main.activity_deployment.*
import org.rfcx.audiomoth.R
import org.rfcx.audiomoth.entity.*
import org.rfcx.audiomoth.localdb.DeploymentDb
import org.rfcx.audiomoth.localdb.LocateDb
import org.rfcx.audiomoth.util.Preferences
import org.rfcx.audiomoth.util.RealmHelper
import org.rfcx.audiomoth.view.configure.*
import org.rfcx.audiomoth.view.configure.PerformBatteryFragment.Companion.TEST_BATTERY
import org.rfcx.audiomoth.view.configure.SyncFragment.Companion.BEFORE_SYNC
import org.rfcx.audiomoth.view.deployment.locate.LocationFragment

class DeploymentActivity : AppCompatActivity(),
    DeploymentProtocol {
    // manager database
    private val realm by lazy { Realm.getInstance(RealmHelper.migrationConfig()) }
    private val deploymentDb by lazy { DeploymentDb(realm) }
    private val locateDb by lazy { LocateDb(realm) }

    private var currentStep = 0
    private var profile: Profile? = null
    private var profileId: String = ""
    private var _deployment: Deployment? = null
    private var _deployLocation: DeploymentLocation? = null
    private var configuration: Configuration? = null

    private val preferences = Preferences.getInstance(this)
    private val name = preferences.getString(Preferences.NICKNAME)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_deployment)
        setupView()
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
        Log.d("DeplaymentActivity", "nextStep next $currentStep")

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

    override fun getProfile(): Profile? {
        return profile
    }

    override fun getProfileId(): String? {
        return profileId
    }

    override fun getDeployment(): Deployment? = this._deployment

    override fun setDeployment(deployment: Deployment) {
        this._deployment = deployment
    }

    override fun geConfiguration(): Configuration? {
        return configuration
    }

    override fun getDeploymentLocation(): DeploymentLocation? = this._deployLocation

    override fun setDeployLocation(locate: Locate) {
        val deployment = _deployment ?: Deployment()
        deployment.state = DeploymentState.Locate.key // state

        val deploymentId = deploymentDb.insertOrUpdateDeployment(deployment, locate)
        locateDb.updateLocate(deploymentId, locate)
    }

    override fun setProfile(profile: Profile) {
        this.profile = profile
    }

    override fun startSetupConfigure(profile: Profile) {
        this.profile = profile
        currentStep = 1
        stepView.go(currentStep, true)
        startFragment(ConfigureFragment.newInstance())
    }

    override fun openSync(status: String) {
        startFragment(SyncFragment.newInstance(status))
    }

    override fun openPerformBattery(status: String, level: Int?) {
        startFragment(PerformBatteryFragment.newInstance(status, level))
    }

    private fun handleFragment(currentStep: Int) {
        // setup fragment for current step
        when (currentStep) {
            0 -> {
                startFragment(LocationFragment.newInstance())
            }
            1 -> {
                startFragment(SelectProfileFragment.newInstance())
            }
            2 -> {
                startFragment(SyncFragment.newInstance(BEFORE_SYNC))
            }
            3 -> {
                startFragment(PerformBatteryFragment.newInstance(TEST_BATTERY, null))
            }
            4 -> {
                startFragment(DeployFragment.newInstance())
            }
        }
    }

    private fun startFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(contentContainer.id, fragment)
            .commit()
    }

    override fun completeStep(images: ArrayList<String>?) {
        nextStep()
//        deployment?.let { MainActivity.startActivity(this, images, deployment.it) }
        finish()
    }

    private fun View.hideKeyboard() = this.let {
        val inputManager =
            context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        inputManager.hideSoftInputFromWindow(windowToken, 0)
    }

    companion object {
        fun startActivity(context: Context) {
            val intent = Intent(context, DeploymentActivity::class.java)
            context.startActivity(intent)
        }
    }
}

interface DeploymentProtocol {
    fun setCompleteTextButton(text: String)
    fun hideCompleteButton()
    fun showCompleteButton()
    fun nextStep()
    fun backStep()
    fun completeStep(images: ArrayList<String>?)

    fun startSetupConfigure(profile: Profile)
    fun openSync(status: String)
    fun openPerformBattery(status: String, level: Int?)

    fun setDeployment(deployment: Deployment)

    fun getProfile(): Profile?
    fun getProfileId(): String?
    fun getDeployment(): Deployment?
    fun geConfiguration(): Configuration?
    fun getDeploymentLocation(): DeploymentLocation?

    fun setDeployLocation(locate: Locate)
    fun setProfile(profile: Profile)
}
