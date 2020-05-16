package org.rfcx.audiomoth.view

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import kotlinx.android.synthetic.main.activity_deployment.*
import org.rfcx.audiomoth.R

class DeploymentActivity : AppCompatActivity(), DeploymentProtocol {

    private var currentStep = 0
    private val steps by lazy { resources.getStringArray(R.array.steps) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_deployment)
        setupView()
    }

    private fun setupView() {
        startFragment(ExampleFragment.newInstance(currentStep)) // start page
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

            /* do something when everything done */
            startFragment(ExampleFragment.newInstance(currentStep))
            hideCompleteButton()
        } else {
            stepView.go(currentStep, true)

            /* show next fragment */
            startFragment(ExampleFragment.newInstance(currentStep))
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

    companion object {
        fun startActivity(context: Context) {
            context.startActivity(Intent(context, DeploymentActivity::class.java))
        }
    }
}

interface DeploymentProtocol {
    fun setCompleteTextButton(text: String)
    fun hideCompleteButton()
    fun showCompleteButton()
    fun nextStep()
    fun backStep()

    fun getNameNextStep(): String // example get data from parent
}
