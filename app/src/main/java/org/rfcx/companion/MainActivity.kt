package org.rfcx.companion

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.snackbar.Snackbar
import io.github.douglasjunior.androidSimpleTooltip.SimpleTooltip
import io.realm.Realm
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.fragment_map.*
import kotlinx.android.synthetic.main.layout_bottom_navigation_menu.*
import org.rfcx.companion.entity.Device
import org.rfcx.companion.entity.response.StreamResponse
import org.rfcx.companion.localdb.EdgeDeploymentDb
import org.rfcx.companion.localdb.LocateDb
import org.rfcx.companion.localdb.LocationGroupDb
import org.rfcx.companion.repo.ApiManager
import org.rfcx.companion.util.*
import org.rfcx.companion.view.deployment.EdgeDeploymentActivity
import org.rfcx.companion.view.deployment.guardian.GuardianDeploymentActivity
import org.rfcx.companion.view.map.DeploymentDetailView
import org.rfcx.companion.view.map.DeploymentViewPagerFragment
import org.rfcx.companion.view.map.MapFragment
import org.rfcx.companion.view.profile.ProfileFragment
import org.rfcx.companion.widget.BottomNavigationMenuItem
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class MainActivity : AppCompatActivity(), MainActivityListener, DeploymentListener {
    private val realm by lazy { Realm.getInstance(RealmHelper.migrationConfig()) }
    private val edgeDeploymentDb by lazy { EdgeDeploymentDb(realm) }
    private val locateDb by lazy { LocateDb(realm) }
    private val locationGroupDb by lazy { LocationGroupDb(realm) }

    private var currentFragment: Fragment? = null
    private val locationPermissions by lazy { LocationPermissions(this) }
    private lateinit var bottomSheetBehavior: BottomSheetBehavior<*>
    private var snackbar: Snackbar? = null
    private var _showDeployments: List<DeploymentDetailView> = listOf()

    private var addTooltip: SimpleTooltip? = null
    private val analytics by lazy { Analytics(this) }

    private var currentSiteLoading = 0

    override fun getShowDeployments(): List<DeploymentDetailView> = this._showDeployments

    override fun setShowDeployments(deployments: List<DeploymentDetailView>) {
        this._showDeployments = deployments
        // delete?
        updateDeploymentDetailPagerView()
    }

    private fun updateDeploymentDetailPagerView() {
        val bottomSheetFragment =
            supportFragmentManager.findFragmentByTag(BOTTOM_SHEET)
        if (bottomSheetFragment != null && bottomSheetFragment is DeploymentViewPagerFragment) {
            bottomSheetFragment.updateItems()
            if (_showDeployments.isEmpty()) {
                hideBottomSheet()
                showBottomAppBar()
            }
        }
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
            if (BuildConfig.ENABLE_GUARDIAN) {
                addTooltip = SimpleTooltip.Builder(this)
                    .arrowColor(ContextCompat.getColor(this, R.color.tooltipColor))
                    .anchorView(createLocationButton)
                    .gravity(Gravity.TOP)
                    .modal(true)
                    .dismissOnInsideTouch(false)
                    .animationPadding(10F)
                    .contentView(R.layout.tooltip_add_device)
                    .animated(false)
                    .transparentOverlay(true)
                    .build()

                addTooltip?.let { tip ->
                    val addEdgeOrAudioMoth =
                        tip.findViewById<ConstraintLayout>(R.id.audioMothLayout)
                    val addGuardian = tip.findViewById<ConstraintLayout>(R.id.guardianLayout)
                    addEdgeOrAudioMoth?.setOnClickListener {
                        EdgeDeploymentActivity.startActivity(this)
                        refreshGettingSites(0, locateDb.getMaxUpdatedAt())
                        tip.dismiss()
                    }
                    addGuardian?.setOnClickListener {
                        GuardianDeploymentActivity.startActivity(this)
                        tip.dismiss()
                    }
                    tip.show()
                }
            } else {
                EdgeDeploymentActivity.startActivity(this)
            }
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
                    val bottomSheetFragment =
                        supportFragmentManager.findFragmentByTag(BOTTOM_SHEET)
                    if (bottomSheetFragment != null) {
                        supportFragmentManager.beginTransaction()
                            .remove(bottomSheetFragment)
                            .commit()
                    }
                }
                if (newState == BottomSheetBehavior.STATE_EXPANDED) {
                    hidBottomAppBar()
                }
            }
        })
    }

    private fun refreshGettingSites(offset: Int, maxUpdatedAt: String?) {
        val token = "Bearer ${this.getIdToken()}"
        ApiManager.getInstance().getDeviceApi().getStreams(
            token,
            SITES_LIMIT_GETTING, offset, maxUpdatedAt, "updated_at"
        )
            .enqueue(object : Callback<List<StreamResponse>> {
                override fun onFailure(call: Call<List<StreamResponse>>, t: Throwable) {
                    if (this@MainActivity.isNetworkAvailable()) {
                        Toast.makeText(
                            this@MainActivity,
                            R.string.error_has_occurred,
                            Toast.LENGTH_SHORT
                        )
                            .show()
                    }
                }

                override fun onResponse(
                    call: Call<List<StreamResponse>>,
                    response: Response<List<StreamResponse>>
                ) {
                    val sites = response.body()
                    sites?.let {
                        it.forEach { item ->
                            locateDb.insertOrUpdate(item)
                        }
                        if (it.size == SITES_LIMIT_GETTING) {
                            currentSiteLoading += SITES_LIMIT_GETTING
                            refreshGettingSites(currentSiteLoading, locateDb.getMaxUpdatedAt())
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
                .arrowColor(ContextCompat.getColor(this, R.color.backgroundColor))
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
        analytics.trackLogoutEvent()
        finish()
    }

    override fun moveMapIntoDeploymentMarker(lat: Double, lng: Double, markerLocationId: String) {
        val mapFragment = supportFragmentManager.findFragmentByTag(MapFragment.tag)
        if (mapFragment is MapFragment) {
            mapFragment.moveToDeploymentMarker(lat, lng, markerLocationId)
        }
    }

    override fun getProjectName(): String {
        val preferences = Preferences.getInstance(this)
        return preferences.getString(Preferences.SELECTED_PROJECT, getString(R.string.none))
    }

    override fun hidBottomAppBar() {
        createLocationButton.visibility = View.INVISIBLE
        bottomBar.visibility = View.INVISIBLE
    }

    override fun showBottomAppBar() {
        bottomBar.visibility = View.VISIBLE
        createLocationButton.visibility = View.VISIBLE
    }

    override fun getBottomSheetState(): Int {
        return bottomSheetBehavior.state
    }

    override fun showBottomSheet(fragment: Fragment) {
        hideSnackbar()
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
        showBottomAppBar()
        bottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
    }

    override fun hideBottomSheetAndBottomAppBar() {
        hidBottomAppBar()
        bottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
    }

    override fun onBackPressed() {
        addTooltip?.dismiss()
        if (bottomSheetBehavior.state == BottomSheetBehavior.STATE_EXPANDED) {
            hideBottomSheet()
            val mapFragment = supportFragmentManager.findFragmentByTag(MapFragment.tag)
            if (mapFragment is MapFragment) {
                mapFragment.clearFeatureSelected()
            }
        } else {
            return super.onBackPressed()
        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        val edgeDeploymentId: String? = intent?.getStringExtra(EXTRA_DEPLOYMENT_ID)

        if (edgeDeploymentId != null) {
            val deployment = edgeDeploymentDb.getDeploymentByDeploymentId(edgeDeploymentId)
            deployment?.let {
                showBottomSheet(
                    DeploymentViewPagerFragment.newInstance(
                        it.id,
                        Device.AUDIOMOTH.value
                    )
                )
            }
        }
    }

    companion object {
        const val EXTRA_DEPLOYMENT_ID = "EXTRA_DEPLOYMENT_ID"
        private const val BOTTOM_SHEET = "BOTTOM_SHEET"
        const val CREATE_DEPLOYMENT_REQUEST_CODE = 1002
        private const val SITES_LIMIT_GETTING = 100

        fun startActivity(context: Context, deploymentId: String? = null) {
            val intent = Intent(context, MainActivity::class.java)
            if (deploymentId != null)
                intent.putExtra(EXTRA_DEPLOYMENT_ID, deploymentId)
            context.startActivity(intent)
        }
    }
}

interface MainActivityListener {
    fun getBottomSheetState(): Int
    fun showBottomSheet(fragment: Fragment)
    fun showBottomAppBar()
    fun hidBottomAppBar()
    fun hideBottomSheet()
    fun hideBottomSheetAndBottomAppBar()
    fun showSnackbar(msg: String, duration: Int)
    fun hideSnackbar()
    fun onLogout()
    fun moveMapIntoDeploymentMarker(lat: Double, lng: Double, markerLocationId: String)
    fun getProjectName(): String
}
