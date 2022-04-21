package org.rfcx.companion.view.deployment

import android.location.Location
import org.rfcx.companion.entity.*
import org.rfcx.companion.service.DownloadStreamState
import org.rfcx.companion.view.deployment.locate.SiteWithLastDeploymentItem

interface BaseDeploymentProtocol {
    fun startMapPicker(latitude: Double, longitude: Double, streamId: Int)
    fun startCheckList()
    fun startDetailDeploymentSite(id: Int, name: String?, isNewSite: Boolean)

    fun isOpenedFromUnfinishedDeployment(): Boolean

    fun nextStep()
    fun backStep()

    fun getDeploymentLocation(): Stream?
    fun getSiteItem(): ArrayList<SiteWithLastDeploymentItem>
    fun getProject(id: Int): Project?
    fun getImages(): List<String>
    fun getCurrentLocation(): Location

    fun setDeployLocation(stream: Stream, isExisted: Boolean)
    fun setSiteItem(items: ArrayList<SiteWithLastDeploymentItem>)
    fun setImages(images: List<String>)
    fun setReadyToDeploy()
    fun setCurrentLocation(location: Location)

    fun handleCheckClicked(number: Int)
    fun getPassedChecks(): List<Int>
    fun setCurrentPage(name: String)

    fun showToolbar()
    fun hideToolbar()
    fun setMenuToolbar(isVisibility: Boolean)
    fun setToolbarTitle()
    fun setToolbarSubtitle(sub: String = "")

    fun isSiteLoading(): DownloadStreamState
    fun showSiteLoadingDialog(text: String)
}
