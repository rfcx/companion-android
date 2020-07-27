package org.rfcx.audiomoth

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.snackbar.Snackbar
import io.github.douglasjunior.androidSimpleTooltip.SimpleTooltip
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.fragment_map.*
import kotlinx.android.synthetic.main.layout_bottom_navigation_menu.*
import org.rfcx.audiomoth.entity.DeploymentLocation
import org.rfcx.audiomoth.util.LocationPermissions
import org.rfcx.audiomoth.util.Preferences
import org.rfcx.audiomoth.util.getUserNickname
import org.rfcx.audiomoth.util.logout
import org.rfcx.audiomoth.view.deployment.DeploymentActivity
import org.rfcx.audiomoth.view.map.DeploymentBottomSheet
import org.rfcx.audiomoth.view.map.MapFragment
import org.rfcx.audiomoth.view.profile.ProfileFragment
import org.rfcx.audiomoth.widget.BottomNavigationMenuItem

class MainActivity : AppCompatActivity(), MainActivityListener, DeploymentListener {
    // database manager
    private var currentFragment: Fragment? = null
    private val locationPermissions by lazy { LocationPermissions(this) }
    private lateinit var bottomSheetBehavior: BottomSheetBehavior<*>

    private var snackbar: Snackbar? = null
    private var _showDeployments: List<DeploymentBottomSheet> = listOf()

    override fun getShowDeployments(): List<DeploymentBottomSheet> = this._showDeployments

    override fun setShowDeployments(deployments: List<DeploymentBottomSheet>) {
        this._showDeployments = deployments
    }

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

        setupSimpleTooltip()
        setupBottomMenu()

        if (savedInstanceState == null) {
            setupFragments()
        }

        bottomSheetBehavior = BottomSheetBehavior.from(bottomSheetContainer)
        bottomSheetBehavior.addBottomSheetCallback(object :
            BottomSheetBehavior.BottomSheetCallback() {
            override fun onSlide(bottomSheet: View, slideOffset: Float) {}

            override fun onStateChanged(bottomSheet: View, newState: Int) {
                if (newState == BottomSheetBehavior.STATE_COLLAPSED) {
                    showBottomAppBar()
                    val bottomSheetFragment =
                        supportFragmentManager.findFragmentByTag(BOTTOM_SHEET)
                    if (bottomSheetFragment != null) {
                        supportFragmentManager.beginTransaction()
                            .remove(bottomSheetFragment)
                            .commit()
                    }
                }
            }
        })
    }

    private fun setupSimpleTooltip() {
        val preferences = Preferences.getInstance(this)
        val isFirstTime = preferences.getBoolean(Preferences.IS_FIRST_TIME, true)

        if (isFirstTime) {
            preferences.putBoolean(Preferences.IS_FIRST_TIME, false)

            SimpleTooltip.Builder(this)
                .arrowColor(ContextCompat.getColor(this, R.color.white))
                .anchorView(createLocationButton)
                .text(getString(R.string.setup_first_device, this.getUserNickname()))
                .gravity(Gravity.TOP)
                .animationPadding(10F)
                .contentView(R.layout.tooltip_custom, R.id.tv_text)
                .animated(true)
                .transparentOverlay(false)
                .build()
                .show()
        }
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
            if (show) resources.getDimensionPixelSize(R.dimen.size_battery_lv_button) else 0
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

    override fun showSnackbar(msg: String, duration: Int) {
        snackbar = Snackbar.make(rootView, msg, duration)
        snackbar?.anchorView = createLocationButton
        snackbar?.show()
    }

    override fun hideSnackbar() {
        snackbar?.dismiss()
    }

    override fun onLogout() {
        this.logout()
        finish()
    }

    override fun moveMapIntoReportMarker(location: DeploymentLocation) {
        val mapFragment = supportFragmentManager.findFragmentByTag(MapFragment.tag)
        if (mapFragment is MapFragment) {
            mapFragment.moveToDeploymentMarker(location)
        }
    }

    override fun hidBottomAppBar() {
        createLocationButton.visibility = View.INVISIBLE
        bottomBar.visibility = View.INVISIBLE
    }

    override fun showBottomAppBar() {
        bottomBar.visibility = View.VISIBLE
        createLocationButton.visibility = View.VISIBLE
    }

    override fun showBottomSheet(fragment: Fragment) {
        Log.d("features", "$fragment")

        hidBottomAppBar()
        val layoutParams: CoordinatorLayout.LayoutParams = bottomSheetContainer.layoutParams
                as CoordinatorLayout.LayoutParams
        layoutParams.anchorGravity = Gravity.BOTTOM
        bottomSheetContainer.layoutParams = layoutParams
        supportFragmentManager.beginTransaction()
            .replace(bottomSheetContainer.id, fragment, BOTTOM_SHEET)
            .commit()
        bottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
    }

    override fun hideBottomSheet() {
        bottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
    }

    override fun onBackPressed() {
        if (bottomSheetBehavior.state == BottomSheetBehavior.STATE_EXPANDED) {
            hideBottomSheet()
        } else {
            return super.onBackPressed()
        }
    }

    companion object {
        private const val IMAGES = "IMAGES"
        private const val DEPLOYMENT_ID = "DEPLOYMENT_ID"
        private const val BOTTOM_SHEET = "BOTTOM_SHEET"

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
    fun showBottomSheet(fragment: Fragment)
    fun showBottomAppBar()
    fun hidBottomAppBar()
    fun hideBottomSheet()
    fun showSnackbar(msg: String, duration: Int)
    fun hideSnackbar()
    fun onLogout()
    fun moveMapIntoReportMarker(location: DeploymentLocation)
}
