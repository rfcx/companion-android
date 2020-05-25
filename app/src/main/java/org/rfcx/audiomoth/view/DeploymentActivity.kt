package org.rfcx.audiomoth.view

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import kotlinx.android.synthetic.main.activity_deployment.*
import kotlinx.android.synthetic.main.fragment_configure.*
import org.rfcx.audiomoth.R
import org.rfcx.audiomoth.entity.*
import org.rfcx.audiomoth.util.Firestore
import org.rfcx.audiomoth.util.Preferences
import org.rfcx.audiomoth.view.configure.*
import org.rfcx.audiomoth.view.configure.PerformBatteryFragment.Companion.TEST_BATTERY
import org.rfcx.audiomoth.view.configure.SyncFragment.Companion.BEFORE_SYNC

class DeploymentActivity : AppCompatActivity(), DeploymentProtocol {

    private var currentStep = 0
    private val steps by lazy { resources.getStringArray(R.array.steps) }
    private var profile: Profile? = null
    private var locate: Locate? = null
    private var profileId: String = ""
    private var locateId: String? = null
    private var deploymentId: String? = null
    private var configuration: Configuration? = null
    private var locationInDeployment: LocationInDeployment? = null

    private val preferences = Preferences.getInstance(this)
    private val guid = preferences.getString(Preferences.USER_GUID)
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

        if (stepView.stepCount == currentStep) {
            stepView.done(true)
            hideCompleteButton()
        } else {
            stepView.go(currentStep, true)
        }

        /* do something when everything done */
        handleFragment(currentStep)
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
            else -> {
                startFragment(ExampleFragment.newInstance(currentStep))
                showDeployButton()
            }
        }
    }

    override fun backStep() {
        stepView.go(stepView.currentStep - 1, true)
    }

    override fun getNameNextStep(): String {
        return if ((currentStep + 1) < stepView.stepCount) steps[currentStep + 1] else "Finish!"
    }

    override fun getProfile(): Profile? {
        return profile
    }

    override fun getProfileId(): String? {
        return profileId
    }

    override fun getDeploymentId(): String? {
        return deploymentId
    }

    override fun geConfiguration(): Configuration? {
        return configuration
    }

    override fun getLocationInDeployment(): LocationInDeployment? {
        return locationInDeployment
    }

    override fun setLocate(locate: Locate) {
        this.locate = locate
    }

    override fun setLocateId(locateId: String) {
        this.locateId = locateId
    }

    override fun setProfile(profile: Profile) {
        this.profile = profile
    }

    override fun openConfigure(profile: Profile) {
        this.profile = profile
        currentStep = 1
        stepView.go(currentStep, true)
        startFragment(ConfigureFragment.newInstance())
    }

    override fun openSync(status: String) {
        startFragment(SyncFragment.newInstance(status))
    }

    override fun openPerformBattery(status: String, image: Int?) {
        startFragment(PerformBatteryFragment.newInstance(status, image))
    }

    override fun saveUser(callback: (Boolean) -> Unit) {
        view1.hideKeyboard()
        if (guid != null && name != null) {
            val user = User(name)
            Firestore(this).saveUser(guid, user) { message, success ->
                callback(success)
                if (!success) {
                    Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun saveLocation(callback: (Boolean) -> Unit) {
        if (guid != null && locate != null) {
            locate?.let {
                Firestore(this).saveLocation(guid, it) { str, success ->
                    callback(success)
                    if (success) {
                        locateId = str
                        setLocationInDeployment(it)
                    } else {
                        Toast.makeText(this, str, Toast.LENGTH_SHORT).show()
                    }
                }
            }
        } else {
            saveProfile {
                if (it) {
                    nextStep()
                }
            }
        }
    }

    override fun updateLocation() {
        if (guid != null && locateId != null && deploymentId != null) {
            locateId?.let { locateId ->
                deploymentId?.let { deploymentId ->
                    Firestore(this).updateLocation(guid, locateId, deploymentId)
                }
            }
        }
    }

    override fun setLocationInDeployment(locate: Locate) {
        if (locateId != null) {
            locationInDeployment = locateId?.let {
                LocationInDeployment(it, locate.name, locate.latitude, locate.longitude)
            }
        }
    }

    override fun saveProfile(callback: (Boolean) -> Unit) {
        if (guid != null && profile != null) {
            profile?.let {
                setConfiguration(it)
                if (it.name.isNotEmpty()) {
                    Firestore(this).saveProfile(guid, it) { str, success ->
                        callback(success)
                        if (success) {
                            if (str != null) {
                                profileId = str
                            }
                        } else {
                            Toast.makeText(this, str, Toast.LENGTH_SHORT).show()
                        }
                    }
                } else {
                    callback(true)
                }
            }
        } else {
            callback(true)
        }
    }

    private fun setConfiguration(profile: Profile) {
        configuration = Configuration(
            profile.gain,
            profile.sampleRate,
            profile.recordingDuration,
            profile.sleepDuration,
            profile.recordingPeriodList,
            profile.durationSelected
        )
    }

    override fun saveDeployment(deployment: Deployment) {
        if (guid != null) {
            Firestore(this).saveDeployment(guid, deployment) { str, success ->
                if (success) {
                    deploymentId = str
                    updateLocation()
                    nextStep()
                } else {
                    Toast.makeText(this, str, Toast.LENGTH_SHORT).show()
                }
            }
        } else {
            nextStep()
            Toast.makeText(this, getString(R.string.error_has_occurred), Toast.LENGTH_SHORT).show()
        }
    }

    private fun startFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(contentContainer.id, fragment)
            .commit()
    }

    private fun showDeployButton() {
        setCompleteTextButton(getString(R.string.deploy))
        showCompleteButton()
        completeStepButton.setOnClickListener {
            nextStep()
            finish()
        }
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

    fun openConfigure(profile: Profile)
    fun openSync(status: String)
    fun openPerformBattery(status: String, image: Int?)

    fun getProfile(): Profile?
    fun getProfileId(): String?
    fun getDeploymentId(): String?
    fun geConfiguration(): Configuration?
    fun getLocationInDeployment(): LocationInDeployment?
    fun getNameNextStep(): String // example get data from parent
    fun setLocate(locate: Locate)
    fun setProfile(profile: Profile)
    fun setLocateId(locateId: String)
    fun setLocationInDeployment(locate: Locate)
    fun updateLocation()

    fun saveUser(callback: (Boolean) -> Unit)
    fun saveLocation(callback: (Boolean) -> Unit)
    fun saveProfile(callback: (Boolean) -> Unit)
    fun saveDeployment(deployment: Deployment)
}
