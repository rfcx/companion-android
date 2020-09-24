package org.rfcx.audiomoth.view.deployment

import org.rfcx.audiomoth.entity.*

interface BaseDeploymentProtocol {
    fun startMapPicker(latitude: Double, longitude: Double, name: String)
    fun startCheckList()

    fun nextStep()
    fun backStep()

    fun getDeploymentLocation(): DeploymentLocation?

    fun setDeployLocation(locate: Locate)
    fun setImages(images: List<String>)
    fun setReadyToDeploy()

    fun handleCheckClicked(number: Int)
    fun getPassedChecks(): List<Int>
    fun setCurrentPage(name: String)

    fun showToolbar()
    fun hideToolbar()
    fun setToolbarTitle()
}
