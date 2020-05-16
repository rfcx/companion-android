package org.rfcx.audiomoth.view

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import kotlinx.android.synthetic.main.activity_deployment.*
import org.rfcx.audiomoth.R
import org.rfcx.audiomoth.view.configure.ConfigureFragment
import org.rfcx.audiomoth.view.configure.LocationFragment
import org.rfcx.audiomoth.view.configure.SelectProfileFragment

class DeploymentActivity : AppCompatActivity(), DeploymentProtocol, UserListener {
    private var currentStep = 0
    private val steps by lazy { resources.getStringArray(R.array.steps) }
    private var userId: String? = null
    private var lastPageInStep: Boolean = true
    private var page: String = LOCATION_FRAGMENT

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_deployment)
        setupView()
        setUserId()
    }

    private fun setUserId() {
        if (intent.hasExtra(USER_ID)) {
            val userId = intent.getStringExtra(USER_ID)
            if (userId != null) {
                this.userId = userId
            }
        }
    }

    private fun setupView() {
        handleFragment(page) // start page
        completeStepButton.visibility = View.GONE
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
        if (lastPageInStep) {
            currentStep += 1
        }

        if (stepView.stepCount == currentStep) {
            stepView.done(true)
            hideCompleteButton()
        } else {
            stepView.go(currentStep, true)
        }

        /* do something when everything done */
        handleFragment(page)
    }

    override fun setLastPageInStep(lastPage: Boolean, nextPage: String) {
        page = nextPage
        lastPageInStep = lastPage
    }

    private fun handleFragment(page: String) {
        // setup fragment for current step
        when (page) {
            LOCATION_FRAGMENT -> {
                startFragment(LocationFragment.newInstance())
            }
            SELECT_PROFILE_FRAGMENT -> {
                startFragment(SelectProfileFragment.newInstance())
            }
            CONFIGURE_FRAGMENT -> {
                startFragment(ConfigureFragment.newInstance())
            }
            EXAMPLE_FRAGMENT -> {
                startFragment(ExampleFragment.newInstance(currentStep))
            }
        }
    }

    override fun backStep() {
        stepView.go(stepView.currentStep - 1, true)
    }

    override fun getNameNextStep(): String {
        return if ((currentStep + 1) < stepView.stepCount) steps[currentStep + 1] else "Finish!"
    }

    private fun startFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(contentContainer.id, fragment)
            .commit()
    }

    override fun getUserId(): String? {
        return userId
    }


    companion object {
        private const val USER_ID = "USER_ID"
        const val LOCATION_FRAGMENT = "LOCATION_FRAGMENT"
        const val SELECT_PROFILE_FRAGMENT = "SELECT_PROFILE_FRAGMENT"
        const val CONFIGURE_FRAGMENT = "CONFIGURE_FRAGMENT"
        const val EXAMPLE_FRAGMENT = "EXAMPLE_FRAGMENT"

        fun startActivity(context: Context, userId: String?) {
            val intent = Intent(context, DeploymentActivity::class.java)
            if (userId != null)
                intent.putExtra(USER_ID, userId)
            context.startActivity(intent)
        }
    }
}

interface DeploymentProtocol {
    fun setCompleteTextButton(text: String)
    fun setLastPageInStep(lastPage: Boolean, nextPage: String)
    fun hideCompleteButton()
    fun showCompleteButton()
    fun nextStep()
    fun backStep()

    fun getNameNextStep(): String // example get data from parent
}

interface UserListener {
    fun getUserId(): String?
}
