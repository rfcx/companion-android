package org.rfcx.audiomoth.view.deployment

import org.rfcx.audiomoth.entity.*

interface BaseDeploymentProtocal {
    fun setCompleteTextButton(text: String)
    fun hideCompleteButton()
    fun showCompleteButton()
    fun nextStep()
    fun backStep()
    fun hideStepView()
    fun showStepView()

    fun getDeploymentLocation(): DeploymentLocation?
    fun setDeployLocation(locate: Locate)

    fun setReadyToDeploy(images: List<String>)
}
