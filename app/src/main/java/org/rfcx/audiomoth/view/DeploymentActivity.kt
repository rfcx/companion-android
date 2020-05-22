package org.rfcx.audiomoth.view

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import kotlinx.android.synthetic.main.activity_deployment.*
import org.rfcx.audiomoth.R
import org.rfcx.audiomoth.entity.Profile
import org.rfcx.audiomoth.entity.User
import org.rfcx.audiomoth.util.Firestore
import org.rfcx.audiomoth.util.Preferences
import org.rfcx.audiomoth.view.configure.*
import org.rfcx.audiomoth.view.configure.PerformBatteryFragment.Companion.TEST_BATTERY
import org.rfcx.audiomoth.view.configure.SyncFragment.Companion.BEFORE_SYNC

class DeploymentActivity : AppCompatActivity(), DeploymentProtocol {

    private var currentStep = 0
    private val steps by lazy { resources.getStringArray(R.array.steps) }
    private var profile: Profile? = null

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

    override fun saveUser() {
        val preferences = Preferences.getInstance(this)
        val guid = preferences.getString(Preferences.USER_GUID)
        val name = preferences.getString(Preferences.NICKNAME)
        if (guid != null && name != null) {
            val user = User(name)
            Firestore().saveUser(guid, user) { message, success ->
                if (success) {
                    nextStep()
                } else {
                    Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
                }
            }
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
    fun getNameNextStep(): String // example get data from parent

    fun saveUser()
}
