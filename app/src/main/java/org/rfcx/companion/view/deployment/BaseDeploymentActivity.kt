package org.rfcx.companion.view.deployment

import android.location.Location
import android.location.LocationManager
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import kotlinx.android.synthetic.main.activity_deployment.*
import kotlinx.android.synthetic.main.toolbar_default.*
import org.rfcx.companion.entity.Stream
import org.rfcx.companion.entity.guardian.Deployment
import org.rfcx.companion.service.DownloadStreamState
import org.rfcx.companion.service.DownloadStreamsWorker
import org.rfcx.companion.view.deployment.locate.MapPickerFragment
import org.rfcx.companion.view.deployment.locate.SiteWithLastDeploymentItem
import org.rfcx.companion.view.deployment.location.DetailDeploymentSiteFragment
import org.rfcx.companion.view.deployment.sync.NewSyncFragment
import org.rfcx.companion.view.detail.MapPickerProtocol
import org.rfcx.companion.view.dialog.CompleteListener
import org.rfcx.companion.view.dialog.SiteLoadingDialogFragment
import java.util.*

abstract class BaseDeploymentActivity :
    AppCompatActivity(),
    CompleteListener,
    BaseDeploymentProtocol,
    MapPickerProtocol {

    var _deployment: Deployment? = null
    var _deployLocation: Stream? = null
    var _images: List<String> = listOf()
    var _stream: Stream? = null
    var _siteItems = arrayListOf<SiteWithLastDeploymentItem>()

    var latitude = 0.0
    var longitude = 0.0
    var nameLocation: String = ""
    var siteId: Int = 0

    var currentCheckName = ""
    var currentLocate: Location? = null

    override fun onSelectedLocation(
        latitude: Double,
        longitude: Double,
        siteId: Int,
        name: String
    ) {
        startFragment(
            DetailDeploymentSiteFragment.newInstance(
                latitude,
                longitude,
                siteId,
                name,
                true
            )
        )
    }

    override fun onAnimationEnd() {
        if (supportFragmentManager.fragments.last() is NewSyncFragment) {
            nextStep()
        } else {
            finish()
        }
    }

    fun startFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(contentContainer.id, fragment)
            .commit()
    }

    private fun setLatLng(latitude: Double, longitude: Double, siteId: Int, name: String) {
        this.latitude = latitude
        this.longitude = longitude
        this.siteId = siteId
        this.nameLocation = name
    }

    override fun startMapPicker(latitude: Double, longitude: Double, siteId: Int, name: String) {
        setLatLng(latitude, longitude, siteId, name)
        startFragment(MapPickerFragment.newInstance(latitude, longitude, siteId, name))
    }

    override fun startDetailDeploymentSite(id: Int, name: String?, isNewSite: Boolean) {
        startFragment(DetailDeploymentSiteFragment.newInstance(id, name, isNewSite))
    }

    override fun getDeploymentLocation(): Stream? = this._deployLocation

    override fun getSiteItem(): ArrayList<SiteWithLastDeploymentItem> = this._siteItems

    override fun getImages(): List<String> {
        return this._images
    }

    override fun getCurrentLocation(): Location =
        currentLocate ?: Location(LocationManager.GPS_PROVIDER)

    override fun setCurrentLocation(location: Location) {
        this.currentLocate = location
    }

    override fun setSiteItem(items: ArrayList<SiteWithLastDeploymentItem>) {
        this._siteItems = items
    }

    override fun setImages(images: List<String>) {
        this._images = images
    }

    override fun showToolbar() {
        toolbar.visibility = View.VISIBLE
    }

    override fun hideToolbar() {
        toolbar.visibility = View.GONE
    }

    override fun setToolbarTitle() {
        supportActionBar?.apply {
            title = currentCheckName
        }
    }

    override fun setCurrentPage(name: String) {
        currentCheckName = name
    }

    override fun isSiteLoading(): DownloadStreamState {
        return DownloadStreamsWorker.isRunning()
    }

    override fun showSiteLoadingDialog(text: String) {
        var siteLoadingDialog: SiteLoadingDialogFragment =
            supportFragmentManager.findFragmentByTag(TAG_SITE_LOADING_DIALOG) as SiteLoadingDialogFragment?
                ?: run {
                    SiteLoadingDialogFragment(text)
                }
        if (siteLoadingDialog.isAdded) {
            siteLoadingDialog.dismiss()
            siteLoadingDialog = SiteLoadingDialogFragment(text)
        }
        siteLoadingDialog.show(
            supportFragmentManager,
            TAG_SITE_LOADING_DIALOG
        )
    }

    companion object {
        const val TAG_SITE_LOADING_DIALOG = "SiteLoadingDialogFragment"
    }
}
