package org.rfcx.audiomoth

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.Transformations
import io.realm.Realm
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.layout_bottom_navigation_menu.*
import org.rfcx.audiomoth.entity.DeploymentImage
import org.rfcx.audiomoth.localdb.DeploymentImageDb
import org.rfcx.audiomoth.util.LocationPermissions
import org.rfcx.audiomoth.util.Preferences
import org.rfcx.audiomoth.util.RealmHelper
import org.rfcx.audiomoth.util.asLiveData
import org.rfcx.audiomoth.view.LoginActivity
import org.rfcx.audiomoth.view.map.MapFragment
import org.rfcx.audiomoth.view.profile.ProfileFragment
import org.rfcx.audiomoth.view.deployment.DeploymentActivity
import org.rfcx.audiomoth.widget.BottomNavigationMenuItem

open class MainActivity : AppCompatActivity(), MainActivityListener {
    // database manager
    private val realm by lazy { Realm.getInstance(RealmHelper.migrationConfig()) }
    private val deploymentImageDb by lazy { DeploymentImageDb(realm) }

    private var currentFragment: Fragment? = null
    private val locationPermissions by lazy { LocationPermissions(this) }

    private lateinit var deployImageLiveData: LiveData<List<DeploymentImage>>

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        locationPermissions.handleActivityResult(requestCode, resultCode)

        currentFragment?.let {
            if (it is MapFragment) {
                it.onActivityResult(requestCode, resultCode, data)
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        locationPermissions.handleRequestResult(requestCode, grantResults)

        currentFragment?.let {
            if (it is MapFragment) {
                it.onRequestPermissionsResult(requestCode, permissions, grantResults)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        createLocationButton.setOnClickListener {
            DeploymentActivity.startActivity(this)
        }

        setupBottomMenu()
        if (savedInstanceState == null) {
            setupFragments()
        }
        fetchData()
    }

    private fun fetchData() {
        deployImageLiveData =
            Transformations.map(deploymentImageDb.getAllResultsAsync().asLiveData()) {
                it
            }
        deployImageLiveData.observeForever(deploymentImageObserver)
    }

    private val deploymentImageObserver = Observer<List<DeploymentImage>> {
        val imageCount = it.size
        val imageUnsentCount = deploymentImageDb.unsentCount().toInt()
        updateSyncingView(imageCount, imageUnsentCount)
    }

    private fun updateSyncingView(imageCount: Int, imageUnsentCount: Int) {
        // TODO: implement logic display syncing view
        imageSyncTextView.visibility = View.GONE

        imageSyncTextView.text = getString(
            if (imageCount > 1) R.string.format_images_unsync else R.string.format_image_unsync,
            imageCount.toString(),
            imageUnsentCount.toString()
        )
    }

    private fun setupBottomMenu() {
        menuMap.setOnClickListener {
            onBottomMenuClick(it)
        }

        menuProfile.setOnClickListener {
            onBottomMenuClick(it)
        }

        menuMap.performClick()
    }

    private fun onBottomMenuClick(menu: View) {
        if ((menu as BottomNavigationMenuItem).menuSelected) return
        when (menu.id) {
            menuMap.id -> {
                menuMap.menuSelected = true
                menuProfile.menuSelected = false

                showMap()
            }

            menuProfile.id -> {
                menuMap.menuSelected = false
                menuProfile.menuSelected = true

                showProfile()
            }
        }
    }

    private fun showProfile() {
        showAboveAppbar(true)
        this.currentFragment = getProfile()
        supportFragmentManager.beginTransaction()
            .show(getProfile())
            .hide(getMap())
            .commit()
    }

    private fun showMap() {
        showAboveAppbar(false)
        this.currentFragment = getMap()
        supportFragmentManager.beginTransaction()
            .show(getMap())
            .hide(getProfile())
            .commit()
    }

    private fun showAboveAppbar(show: Boolean) {
        val contentContainerPaddingBottom =
            if (show) resources.getDimensionPixelSize(R.dimen.button_battery_lv) else 0
        contentContainer.setPadding(0, 0, 0, contentContainerPaddingBottom)
    }

    private fun getMap(): MapFragment =
        supportFragmentManager.findFragmentByTag(MapFragment.tag) as MapFragment?
            ?: MapFragment.newInstance()

    private fun getProfile(): ProfileFragment =
        supportFragmentManager.findFragmentByTag(ProfileFragment.tag) as ProfileFragment?
            ?: ProfileFragment.newInstance()

    private fun setupFragments() {
        supportFragmentManager.beginTransaction()
            .add(contentContainer.id, getProfile(), ProfileFragment.tag)
            .add(contentContainer.id, getMap(), MapFragment.tag)
            .commit()

        menuMap.performClick()
    }

    override fun onDestroy() {
        super.onDestroy()
        deployImageLiveData.removeObserver(deploymentImageObserver)
    }

    override fun onLogout() {
        Preferences.getInstance(this).clear()
        LoginActivity.startActivity(this)
        finish()
    }

    companion object {
        private const val IMAGES = "IMAGES"
        private const val DEPLOYMENT_ID = "DEPLOYMENT_ID"

        fun startActivity(
            context: Context,
            images: ArrayList<String>? = null,
            deploymentId: String? = null
        ) {
            val intent = Intent(context, MainActivity::class.java)
            intent.putStringArrayListExtra(IMAGES, images)
            intent.putExtra(DEPLOYMENT_ID, deploymentId)
            context.startActivity(intent)
        }
    }
}

interface MainActivityListener {
    fun onLogout()
}
