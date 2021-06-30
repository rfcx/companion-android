package org.rfcx.companion.view.deployment.songmeter

import android.content.Context
import android.content.Intent
import android.location.Location
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import io.realm.RealmList
import kotlinx.android.synthetic.main.activity_song_meter_deployment.*
import kotlinx.android.synthetic.main.toolbar_default.*
import org.rfcx.companion.R
import org.rfcx.companion.base.SongMeterViewModelFactory
import org.rfcx.companion.entity.*
import org.rfcx.companion.service.DownloadStreamState
import org.rfcx.companion.view.deployment.DeployFragment
import org.rfcx.companion.view.deployment.locate.SiteWithLastDeploymentItem

class SongMeterDeploymentActivity : AppCompatActivity(), SongMeterDeploymentProtocol {

    private var _images: List<String> = listOf()

    private var currentCheck = 0
    private var currentCheckName = ""
    private var passedChecks = RealmList<Int>()

    private lateinit var songMeterViewModel: SongMeterViewModel

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
        setViewModel()
    }

    private fun setViewModel() {
        songMeterViewModel = ViewModelProvider(
            this,
            SongMeterViewModelFactory(application)
        ).get(SongMeterViewModel::class.java)
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
        if (passedChecks.contains(2) && _images.isNullOrEmpty()) {
            passedChecks.remove(2)
        }

        if (currentCheck !in passedChecks) {
            if (currentCheck == 2 && _images.isNullOrEmpty()) {
                startCheckList()
                return
            } else {
                passedChecks.add(currentCheck)
            }
        }
        startCheckList()
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
        return this._images
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
        this._images = images
    }

    override fun setReadyToDeploy() {
        TODO("Not yet implemented")
    }

    override fun setCurrentLocation(location: Location) {
        TODO("Not yet implemented")
    }

    override fun handleCheckClicked(number: Int) {
        // setup fragment for current step
        currentCheck = number
        when (number) {
            0 -> {
                // TODO:: Set deployment location
            }
            1 -> {
                // TODO:: Sync process
            }
            2 -> {
                startFragment(DeployFragment.newInstance(Screen.SONG_METER_CHECK_LIST.id))
            }
        }
    }

    override fun getPassedChecks(): List<Int> = passedChecks

    override fun setCurrentPage(name: String) {
        currentCheckName = name
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
