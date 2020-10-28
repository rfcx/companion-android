package org.rfcx.companion.view.deployment

import org.rfcx.companion.entity.*

interface BaseDeploymentProtocol {
    fun startMapPicker(latitude: Double, longitude: Double, name: String)
    fun startCheckList()

    fun nextStep()
    fun backStep()

    fun getDeploymentLocation(): DeploymentLocation?
    fun getLocationGroup(name: String): LocationGroups?

    fun setDeployLocation(locate: Locate, isExisted: Boolean)
    fun setImages(images: List<String>)
    fun setReadyToDeploy()

    fun handleCheckClicked(number: Int)
    fun getPassedChecks(): List<Int>
    fun setCurrentPage(name: String)

    fun showToolbar()
    fun hideToolbar()
    fun setToolbarTitle()
}
