package org.rfcx.companion.view.deployment.songmeter

import android.content.Context
import android.content.Intent
import android.location.Location
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.fragment.app.Fragment
import kotlinx.android.synthetic.main.activity_song_meter_deployment.*
import kotlinx.android.synthetic.main.toolbar_default.*
import org.rfcx.companion.R
import org.rfcx.companion.entity.DeploymentLocation
import org.rfcx.companion.entity.Locate
import org.rfcx.companion.entity.Project
import org.rfcx.companion.service.DownloadStreamState
import org.rfcx.companion.view.deployment.locate.SiteWithLastDeploymentItem

class SongMeterDeploymentActivity : AppCompatActivity(), SongMeterDeploymentProtocol {

    companion object {
        const val TAG = "SongMeterDeploymentActivity"

        fun startActivity(context: Context) {
            val intent = Intent(context, SongMeterDeploymentActivity::class.java)
            context.startActivity(intent)
        }
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_song_meter_deployment)

        setupToolbar()
        startCheckList()
    }

    override fun startMapPicker(latitude: Double, longitude: Double, siteId: Int, name: String) {
        TODO("Not yet implemented")
    }

    override fun startCheckList() {
        startFragment(SongMeterCheckListFragment.newInstance())
    }

    override fun startDetailDeploymentSite(id: Int, name: String?, isNewSite: Boolean) {
        TODO("Not yet implemented")
    }

    override fun isOpenedFromUnfinishedDeployment(): Boolean {
        TODO("Not yet implemented")
    }

    override fun nextStep() {
        TODO("Not yet implemented")
    }

    override fun backStep() {
        TODO("Not yet implemented")
    }

    override fun getDeploymentLocation(): DeploymentLocation? {
        TODO("Not yet implemented")
    }

    override fun getSiteItem(): ArrayList<SiteWithLastDeploymentItem> {
        TODO("Not yet implemented")
    }

    override fun getLocationGroup(name: String): Project? {
        TODO("Not yet implemented")
    }

    override fun getImages(): List<String> {
        TODO("Not yet implemented")
    }

    override fun getCurrentLocation(): Location {
        TODO("Not yet implemented")
    }

    override fun setDeployLocation(locate: Locate, isExisted: Boolean) {
        TODO("Not yet implemented")
    }

    override fun setSiteItem(items: ArrayList<SiteWithLastDeploymentItem>) {
        TODO("Not yet implemented")
    }

    override fun setImages(images: List<String>) {
        TODO("Not yet implemented")
    }

    override fun setReadyToDeploy() {
        TODO("Not yet implemented")
    }

    override fun setCurrentLocation(location: Location) {
        TODO("Not yet implemented")
    }

    override fun handleCheckClicked(number: Int) {
        TODO("Not yet implemented")
    }

    override fun getPassedChecks(): List<Int> {
        TODO("Not yet implemented")
    }

    override fun setCurrentPage(name: String) {
        TODO("Not yet implemented")
    }

    override fun showToolbar() {
        TODO("Not yet implemented")
    }

    override fun hideToolbar() {
        TODO("Not yet implemented")
    }

    override fun setToolbarTitle() {
        TODO("Not yet implemented")
    }

    override fun isSiteLoading(): DownloadStreamState {
        TODO("Not yet implemented")
    }

    override fun showSiteLoadingDialog(text: String) {
        TODO("Not yet implemented")
    }

    private fun startFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(contentContainer.id, fragment)
            .commit()
    }

    private fun setupToolbar() {
        setSupportActionBar(toolbar)
        supportActionBar?.apply {
            setDisplayShowTitleEnabled(true)
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowHomeEnabled(true)
        }
    }
}
