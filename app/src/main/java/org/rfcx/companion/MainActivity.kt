package org.rfcx.companion

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.IntentSender
import android.os.Bundle
import android.view.Gravity
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.snackbar.Snackbar
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.install.InstallState
import com.google.android.play.core.install.InstallStateUpdatedListener
import com.google.android.play.core.install.model.ActivityResult
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.InstallStatus
import com.google.android.play.core.install.model.UpdateAvailability
import io.github.douglasjunior.androidSimpleTooltip.SimpleTooltip
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.fragment_map.*
import kotlinx.android.synthetic.main.layout_bottom_navigation_menu.*
import kotlinx.android.synthetic.main.layout_search_view.*
import org.rfcx.companion.base.ViewModelFactory
import org.rfcx.companion.entity.CrashlyticsKey
import org.rfcx.companion.entity.Stream
import org.rfcx.companion.entity.isGuest
import org.rfcx.companion.repo.api.CoreApiHelper
import org.rfcx.companion.repo.api.CoreApiServiceImpl
import org.rfcx.companion.repo.api.DeviceApiHelper
import org.rfcx.companion.repo.api.DeviceApiServiceImpl
import org.rfcx.companion.repo.local.LocalDataHelper
import org.rfcx.companion.service.DeploymentCleanupWorker
import org.rfcx.companion.util.*
import org.rfcx.companion.view.deployment.AudioMothDeploymentActivity
import org.rfcx.companion.view.deployment.guardian.GuardianDeploymentActivity
import org.rfcx.companion.view.map.MapFragment
import org.rfcx.companion.view.profile.ProfileFragment
import org.rfcx.companion.view.project.ProjectSelectActivity
import org.rfcx.companion.widget.BottomNavigationMenuItem

class MainActivity : AppCompatActivity(), MainActivityListener, InstallStateUpdatedListener {
    private lateinit var mainViewModel: MainViewModel

    private var currentFragment: Fragment? = null
    private val locationPermissions by lazy { LocationPermissions(this) }
    private lateinit var bottomSheetBehavior: BottomSheetBehavior<*>
    private var snackbar: Snackbar? = null

    private var addTooltip: SimpleTooltip? = null
    private val analytics by lazy { Analytics(this) }
    private val firebaseCrashlytics by lazy { Crashlytics() }

    private val appUpdateManager by lazy {
        AppUpdateManagerFactory.create(this)
    }

    override fun onResume() {
        super.onResume()
        appUpdateManager.appUpdateInfo.addOnSuccessListener { appUpdateInfo ->
            if (appUpdateInfo.installStatus() == InstallStatus.DOWNLOADED) {
                showSnackbarForCompleteUpdate()
            }

            try {
                if (appUpdateInfo.updateAvailability() == UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS) {
                    // If an in-app update is already running, resume the update.
                    appUpdateManager.startUpdateFlowForResult(
                        appUpdateInfo,
                        AppUpdateType.IMMEDIATE,
                        this,
                        UPDATE_REQUEST_CODE
                    )
                }
            } catch (e: IntentSender.SendIntentException) {
                e.printStackTrace()
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == UPDATE_REQUEST_CODE) {
            when (resultCode) {
                Activity.RESULT_CANCELED -> showSnackbarForUpdateState("Updating is canceled")
                ActivityResult.RESULT_IN_APP_UPDATE_FAILED -> showSnackbarForUpdateState("Download update is failed")
            }
        } else {
            locationPermissions.handleActivityResult(requestCode, resultCode)

            currentFragment?.let {
                if (it is MapFragment) {
                    it.onActivityResult(requestCode, resultCode, data)
                }
            }
        }
    }

    private fun setViewModel() {
        mainViewModel = ViewModelProvider(
            this,
            ViewModelFactory(
                application,
                DeviceApiHelper(DeviceApiServiceImpl()),
                CoreApiHelper(CoreApiServiceImpl()),
                LocalDataHelper()
            )
        ).get(MainViewModel::class.java)
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
        checkInAppUpdate()
        setViewModel()

        firebaseCrashlytics.setCustomKey(CrashlyticsKey.EmailUser.key, this.getEmailUser())

        val preferences = Preferences.getInstance(this)
        val projectId = preferences.getInt(Preferences.SELECTED_PROJECT)
        val project = mainViewModel.getProjectById(projectId)
        if (project == null) {
            logout()
        }
        project?.let {
            if (it.isGuest()) {
                preferences.putInt(Preferences.SELECTED_PROJECT, -1)
                ProjectSelectActivity.startActivity(this)
                finish()
            }
        }

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
                        AudioMothDeploymentActivity.startActivity(this)
                        tip.dismiss()
                    }
                    addGuardian?.setOnClickListener {
                        GuardianDeploymentActivity.startActivity(this)
                        tip.dismiss()
                    }
                    tip.show()
                }
            } else {
                AudioMothDeploymentActivity.startActivity(this)
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
                        hideBottomAppBar()
                    }
                }
            })
    }

    private fun checkInAppUpdate() {
        val appUpdateInfoTask = appUpdateManager.appUpdateInfo
        appUpdateInfoTask.addOnSuccessListener { appUpdateInfo ->
            if (appUpdateInfo.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE
            ) {
                if (appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE) && (appUpdateInfo.clientVersionStalenessDays() ?: -1) >= 10) {
                    appUpdateManager.startUpdateFlowForResult(
                        appUpdateInfo,
                        AppUpdateType.IMMEDIATE,
                        this,
                        UPDATE_REQUEST_CODE
                    )
                    appUpdateManager.registerListener(this)
                }
            }

            if (appUpdateInfo.installStatus() == InstallStatus.INSTALLED) {
                showSnackbarForUpdateState("An update has just been downloaded.")
            }
        }
    }

    override fun onStateUpdate(state: InstallState) {
        if (state.installStatus() == InstallStatus.DOWNLOADED) {
            showSnackbarForCompleteUpdate()
        } else if (state.installStatus() == InstallStatus.INSTALLED) {
            showSnackbarForUpdateState("Update success")
            appUpdateManager.unregisterListener(this@MainActivity)
        }
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
        snackbar = Snackbar.make(mainRootView, msg, duration)
        snackbar?.anchorView = createLocationButton
        snackbar?.show()
    }

    override fun showSnackbarForUpdateState(msg: String) {
        snackbar = Snackbar.make(mainRootView, msg, Snackbar.LENGTH_LONG)
        snackbar?.show()
    }

    override fun showSnackbarForCompleteUpdate() {
        snackbar = Snackbar.make(mainRootView, "Update is successfully downloaded", Snackbar.LENGTH_INDEFINITE)
            .apply {
                setAction("RESTART") {
                    appUpdateManager.completeUpdate()
                    appUpdateManager.unregisterListener(this@MainActivity)
                }
                show()
            }
        snackbar?.show()
    }

    override fun hideSnackbar() {
        snackbar?.dismiss()
    }

    override fun onLogout() {
        DeploymentCleanupWorker.stopAllWork(this)
        LocationTracking.set(this, false)
        mainViewModel.onDestroy()
        this.logout()
        analytics.trackLogoutEvent()
        finish()
    }

    override fun moveMapIntoDeploymentMarker(lat: Double, lng: Double, markerLocationId: String) {
        hideBottomAppBar()
        val mapFragment = supportFragmentManager.findFragmentByTag(MapFragment.tag)
        if (mapFragment is MapFragment) {
            mapFragment.moveToDeploymentMarker(lat, lng)
        }
    }

    override fun showTrackOnMap(site: Stream?, markerLocationId: String) {
        val mapFragment = supportFragmentManager.findFragmentByTag(MapFragment.tag)
        if (mapFragment is MapFragment) {
            site?.let {
                mapFragment.gettingTracksAndMoveToPin(it, markerLocationId)
            }
        }
    }

    override fun getProjectName(): String {
        val preferences = Preferences.getInstance(this)
        val projectId = preferences.getInt(Preferences.SELECTED_PROJECT)
        val project = mainViewModel.getProjectById(projectId)
        return project?.name ?: getString(R.string.none)
    }

    override fun hideBottomAppBar() {
        createLocationButton.visibility = View.GONE
        bottomBar.visibility = View.GONE

        val mapFragment = supportFragmentManager.findFragmentByTag(MapFragment.tag)
        if (mapFragment is MapFragment) {
            mapFragment.hideButtonOnMap()
        }
    }

    override fun showBottomAppBar() {
        bottomBar.visibility = View.VISIBLE
        createLocationButton.visibility = View.VISIBLE

        val mapFragment = supportFragmentManager.findFragmentByTag(MapFragment.tag)
        if (mapFragment is MapFragment) {
            mapFragment.showButtonOnMap()
        }
    }

    override fun getBottomSheetState(): Int {
        return bottomSheetBehavior.state
    }

    override fun showBottomSheet(fragment: Fragment) {
        hideSnackbar()
        hideBottomAppBar()
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
        hideBottomAppBar()
        bottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
    }

    override fun onBackPressed() {
        addTooltip?.dismiss()
        when {
            projectRecyclerView.visibility == View.VISIBLE -> {
                projectRecyclerView.visibility = View.GONE
                projectSwipeRefreshView.visibility = View.GONE
                setSearchBar()
            }
            searchLayout.visibility == View.VISIBLE -> {
                siteSwipeRefreshView.visibility = View.GONE
                setSearchBar()
            }
            bottomSheetBehavior.state == BottomSheetBehavior.STATE_EXPANDED -> {
                hideBottomSheet()
                clearFeatureSelectedOnMap()
            }
            else -> {
                return super.onBackPressed()
            }
        }
    }

    private fun setSearchBar() {
        clearFeatureSelectedOnMap()
        val mapFragment = supportFragmentManager.findFragmentByTag(MapFragment.tag)
        if (mapFragment is MapFragment) {
            mapFragment.showSearchBar(false)
        }
    }

    override fun clearFeatureSelectedOnMap() {
        val mapFragment = supportFragmentManager.findFragmentByTag(MapFragment.tag)
        if (mapFragment is MapFragment) {
            mapFragment.clearFeatureSelected()
        }
    }

    override fun onDestroy() {
        appUpdateManager.unregisterListener(this)
        super.onDestroy()
    }

    companion object {
        const val EXTRA_DEPLOYMENT_ID = "EXTRA_DEPLOYMENT_ID"
        private const val BOTTOM_SHEET = "BOTTOM_SHEET"
        private const val UPDATE_REQUEST_CODE = 10000

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
    fun hideBottomAppBar()
    fun hideBottomSheet()
    fun hideBottomSheetAndBottomAppBar()
    fun showSnackbar(msg: String, duration: Int)
    fun showSnackbarForUpdateState(msg: String)
    fun showSnackbarForCompleteUpdate()
    fun hideSnackbar()
    fun onLogout()
    fun moveMapIntoDeploymentMarker(lat: Double, lng: Double, markerLocationId: String)
    fun showTrackOnMap(site: Stream?, markerLocationId: String)
    fun getProjectName(): String
    fun clearFeatureSelectedOnMap()
}
