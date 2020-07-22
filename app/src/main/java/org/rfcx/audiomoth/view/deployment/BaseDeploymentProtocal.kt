package org.rfcx.audiomoth.view.deployment

import org.rfcx.audiomoth.entity.*

interface BaseDeploymentProtocal {
    fun startMapPicker(latitude: Double, longitude: Double)
    fun startLocation(latitude: Double, longitude: Double)

    fun hideCompleteButton()
    fun showCompleteButton()
    fun nextStep()
    fun backStep()
    fun hideStepView()
    fun showStepView()

    fun getDeploymentLocation(): DeploymentLocation?

    fun setDeployLocation(locate: Locate)
    fun setCompleteTextButton(text: String)
    fun setReadyToDeploy(images: List<String>)
}
