package org.rfcx.audiomoth.view.deployment

import org.rfcx.audiomoth.entity.*

interface BaseDeploymentProtocol {
    fun startMapPicker(latitude: Double, longitude: Double, name: String)
    fun startLocation(latitude: Double, longitude: Double, name: String)

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
