package org.rfcx.companion.view.deployment

import android.location.Location
import org.rfcx.companion.entity.*
import org.rfcx.companion.service.DownloadStreamState
import org.rfcx.companion.view.deployment.locate.SiteWithLastDeploymentItem

interface BaseDeploymentProtocol {
    fun startMapPicker(latitude: Double, longitude: Double, altitude: Double, streamId: Int, streamName: String)
    fun startCheckList()
    fun startDetailDeploymentSite(id: Int, streamName: String, isNewSite: Boolean)
    fun startDetailDeploymentSite(lat: Double, lng: Double, siteId: Int, siteName: String)

    fun nextStep()
    fun backStep()

    fun getDeploymentStream(): Stream?
    fun getSiteItem(): List<SiteWithLastDeploymentItem>
    fun getStream(id: Int): Stream?
    fun getProject(id: Int): Project?
    fun getImages(): List<String>
    fun getCurrentLocation(): Location

    fun setDeployLocation(stream: Stream, isExisted: Boolean)
    fun setSiteItem(items: List<SiteWithLastDeploymentItem>)
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
