package org.rfcx.audiomoth.view

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import kotlinx.android.synthetic.main.activity_deployment.*
import org.rfcx.audiomoth.R
import org.rfcx.audiomoth.entity.Profile
import org.rfcx.audiomoth.view.configure.ConfigureFragment
import org.rfcx.audiomoth.view.configure.LocationFragment
import org.rfcx.audiomoth.view.configure.SelectProfileFragment
import org.rfcx.audiomoth.view.configure.SyncFragment
import org.rfcx.audiomoth.view.configure.SyncFragment.Companion.BEFORE_SYNC

class DeploymentActivity : AppCompatActivity(), DeploymentProtocol{
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

    private fun startFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(contentContainer.id, fragment)
            .commit()
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

    fun getProfile(): Profile?
    fun getNameNextStep(): String // example get data from parent
}
