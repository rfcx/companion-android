package org.rfcx.audiomoth

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.layout_bottom_navigation_menu.*
import org.rfcx.audiomoth.util.LocationPermissions
import org.rfcx.audiomoth.util.Storage
import org.rfcx.audiomoth.view.DeploymentActivity
import org.rfcx.audiomoth.view.configure.MapFragment
import org.rfcx.audiomoth.view.configure.ProfileFragment
import org.rfcx.audiomoth.widget.BottomNavigationMenuItem

open class MainActivity : AppCompatActivity() {

    private var currentFragment: Fragment? = null
    private val locationPermissions by lazy { LocationPermissions(this) }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        locationPermissions.handleActivityResult(requestCode, resultCode)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        locationPermissions.handleRequestResult(requestCode, grantResults)
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

//        logoutImageView.setOnClickListener {
//            Preferences.getInstance(this).clear()
//            LoginActivity.startActivity(this)
//            finish()
//        }
        setSyncImage()
        logoutImageView.visibility = View.GONE
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

    private fun setSyncImage() {
        val images = intent.extras?.getStringArrayList(IMAGES)
        val deploymentId = intent.extras?.getString(DEPLOYMENT_ID)
        if (!images.isNullOrEmpty()) {
            if (deploymentId != null) {
                Storage(this).uploadImage(images, deploymentId) { count, unSyncNum ->
                    if (count == 1) {
                        if (unSyncNum == 0) {
                            photoSyncTextView.visibility = View.GONE
                        } else {
                            photoSyncTextView.visibility = View.VISIBLE
                            photoSyncTextView.text = getString(
                                R.string.format_image_unsync,
                                count.toString(),
                                unSyncNum.toString()
                            )
                        }
                    } else {
                        if (unSyncNum == 0) {
                            photoSyncTextView.visibility = View.GONE
                        } else {
                            photoSyncTextView.visibility = View.VISIBLE
                            photoSyncTextView.text = getString(
                                R.string.format_images_unsync,
                                count.toString(),
                                unSyncNum.toString()
                            )
                        }
                    }
                }
            }
        } else {
            photoSyncTextView.visibility = View.GONE
        }
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
