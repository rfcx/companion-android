package org.rfcx.companion.view.deployment

import android.location.Location
import org.rfcx.companion.entity.*
import org.rfcx.companion.service.DownloadStreamState

interface BaseDeploymentProtocol {
    fun startMapPicker(latitude: Double, longitude: Double, altitude: Double, name: String)
    fun startCheckList()
    fun startSelectingExistedSite(latitude: Double, longitude: Double)
    fun startDetailDeploymentSite(id: Int, name: String?, isNewSite: Boolean)

    fun isOpenedFromUnfinishedDeployment(): Boolean

    fun nextStep()
    fun backStep()

    fun getDeploymentLocation(): DeploymentLocation?
    fun getLocationGroup(name: String): LocationGroups?
    fun getImages(): List<String>
    fun getCurrentLocation(): Location

    fun setDeployLocation(locate: Locate, isExisted: Boolean)
    fun setImages(images: List<String>)
    fun setReadyToDeploy()
    fun setCurrentLocation(location: Location)


    fun handleCheckClicked(number: Int)
    fun getPassedChecks(): List<Int>
    fun setCurrentPage(name: String)

    fun showToolbar()
    fun hideToolbar()
    fun setToolbarTitle()

    fun isSiteLoading(): DownloadStreamState
    fun showSiteLoadingDialog(text: String)
}
