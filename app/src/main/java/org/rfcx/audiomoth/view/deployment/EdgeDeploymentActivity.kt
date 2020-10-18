package org.rfcx.audiomoth.view.deployment

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import io.realm.Realm
import kotlinx.android.synthetic.main.activity_deployment.*
import kotlinx.android.synthetic.main.toolbar_default.*
import org.rfcx.audiomoth.BuildConfig
import org.rfcx.audiomoth.R
import org.rfcx.audiomoth.entity.DeploymentLocation
import org.rfcx.audiomoth.entity.DeploymentState
import org.rfcx.audiomoth.entity.EdgeDeployment
import org.rfcx.audiomoth.entity.Locate
import org.rfcx.audiomoth.localdb.DeploymentImageDb
import org.rfcx.audiomoth.localdb.EdgeDeploymentDb
import org.rfcx.audiomoth.localdb.LocateDb
import org.rfcx.audiomoth.entity.*
import org.rfcx.audiomoth.localdb.*
import org.rfcx.audiomoth.service.DeploymentSyncWorker
import org.rfcx.audiomoth.util.AudioMothChimeConnector
import org.rfcx.audiomoth.util.RealmHelper
import org.rfcx.audiomoth.view.deployment.guardian.GuardianDeploymentActivity
import org.rfcx.audiomoth.view.deployment.locate.LocationFragment
import org.rfcx.audiomoth.view.deployment.locate.MapPickerFragment
import org.rfcx.audiomoth.view.deployment.sync.SyncFragment
import org.rfcx.audiomoth.view.deployment.sync.SyncFragment.Companion.BEFORE_SYNC
import org.rfcx.audiomoth.view.detail.MapPickerProtocol
import org.rfcx.audiomoth.view.dialog.CompleteFragment
import org.rfcx.audiomoth.view.dialog.CompleteListener
import org.rfcx.audiomoth.view.dialog.LoadingDialogFragment
import java.util.*

class EdgeDeploymentActivity : AppCompatActivity(), EdgeDeploymentProtocol, CompleteListener,
    MapPickerProtocol {
    // manager database
    private val realm by lazy { Realm.getInstance(RealmHelper.migrationConfig()) }
    private val deploymentDb by lazy {
        EdgeDeploymentDb(
            realm
        )
    }
    private val locateDb by lazy { LocateDb(realm) }
    private val locationGroupDb by lazy { LocationGroupDb(realm) }
    private val deploymentImageDb by lazy { DeploymentImageDb(realm) }

    private var _deployment: EdgeDeployment? = null
    private var _deployLocation: DeploymentLocation? = null
    private var _images: List<String> = listOf()
    private var _deployLocationGroup: LocationGroup? = null

    private var audioMothConnector = AudioMothChimeConnector()
    private var calendar = Calendar.getInstance()

    private var latitude = 0.0
    private var longitude = 0.0
    private var nameLocation: String = ""

    private var currentCheck = 0
    private var currentCheckName = ""
    private var passedChecks = arrayListOf<Int>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_deployment)

        setupToolbar()

        val deploymentId = intent.extras?.getInt(EXTRA_DEPLOYMENT_ID)
        if (deploymentId != null) {
            handleDeploymentStep(deploymentId)
        } else {
            if (BuildConfig.ENABLE_GUARDIAN) {
                startFragment(ChooseDeviceFragment.newInstance())
            } else {
                openWithEdgeDevice()
            }
        }
    }

    private fun setupToolbar() {
        setSupportActionBar(toolbar)
        supportActionBar?.apply {
            setDisplayShowTitleEnabled(true)
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowHomeEnabled(true)
        }
    }

    override fun openWithEdgeDevice() {
        startCheckList()
    }

    override fun openWithGuardianDevice() {
        GuardianDeploymentActivity.startActivity(this)
        finish()
    }

    override fun onSupportNavigateUp(): Boolean {
        backStep()
        return true
    }

    override fun nextStep() {
        if (currentCheck !in passedChecks) {
            passedChecks.add(currentCheck)
        }
        startCheckList()
    }

    override fun backStep() {
        val container = supportFragmentManager.findFragmentById(R.id.contentContainer)
        when (container) {
            is MapPickerFragment -> startFragment(LocationFragment.newInstance())
            is EdgeCheckListFragment -> {
                passedChecks.clear() // remove all passed
                finish()
            }
            is LocationFragment -> {
                val isFragmentPopped = handleNestedFragmentBackStack(supportFragmentManager)
                if (!isFragmentPopped && supportFragmentManager.backStackEntryCount <= 1) {
                    // if top's fragment is  LocationFragment then finish else show LocationFragment fragment
                    if (supportFragmentManager.fragments.firstOrNull() is LocationFragment) {
                        startCheckList()
                    } else {
                        startLocationPage(this.latitude, this.longitude, this.nameLocation)
                    }
                } else if (!isFragmentPopped) {
                    super.onBackPressed()
                }
            }
            is ChooseDeviceFragment -> finish()
            else -> startCheckList()
        }
    }

    override fun startCheckList() {
        startFragment(EdgeCheckListFragment.newInstance())
    }

    override fun getDeployment(): EdgeDeployment? = this._deployment ?: EdgeDeployment()

    override fun getLocationGroup(name: String): LocationGroups? {
        return locationGroupDb.getLocationGroup(name)
    }

    override fun setDeployment(deployment: EdgeDeployment) {
        this._deployment = deployment
    }

    override fun getDeploymentLocation(): DeploymentLocation? = this._deployLocation

    override fun setDeployLocation(locate: Locate) {
        val deployment = _deployment ?: EdgeDeployment()
        deployment.state = DeploymentState.Edge.Locate.key // state

        this._deployLocation = locate.asDeploymentLocation()
        val deploymentId = deploymentDb.insertOrUpdate(deployment, _deployLocation!!)
        locateDb.insertOrUpdateLocate(deploymentId, locate) // update locate - last deployment
        setDeployment(deployment)
    }

    override fun setImages(images: List<String>) {
        this._images = images
    }

    private fun setLatLng(latitude: Double, longitude: Double, name: String) {
        this.latitude = latitude
        this.longitude = longitude
        this.nameLocation = name
    }

    override fun setReadyToDeploy() {
        showLoading()
        _deployment?.let {
            it.deployedAt = Date()
            it.updatedAt = Date()
            it.state = DeploymentState.Edge.ReadyToUpload.key
            setDeployment(it)

            deploymentImageDb.insertImage(it, this._images)
            deploymentDb.updateDeployment(it)

            DeploymentSyncWorker.enqueue(this@EdgeDeploymentActivity)
            hideLoading()
            showComplete()
        }
    }

    override fun handleCheckClicked(number: Int) {
        // setup fragment for current step
        currentCheck = number
        when (number) {
            0 -> {
                updateDeploymentState(DeploymentState.Edge.Locate)
                startFragment(LocationFragment.newInstance())
            }
            1 -> {
                updateDeploymentState(DeploymentState.Edge.Sync)
                startFragment(SyncFragment.newInstance(BEFORE_SYNC))
            }
            2 -> {
                updateDeploymentState(DeploymentState.Edge.Deploy)
                startFragment(DeployFragment.newInstance())
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

    override fun startSyncing(status: String) {
        startFragment(SyncFragment.newInstance(status))
    }

    override fun startLocationPage(latitude: Double, longitude: Double, name: String) {
        startFragment(LocationFragment.newInstance(latitude, longitude, name))
    }

    override fun playSyncSound() {
        val deploymentId = getDeployment()?.deploymentId
        val deploymentIdArrayInt = deploymentId?.map { it.toInt() }?.toTypedArray() ?: arrayOf()
        Thread {
            audioMothConnector.playTimeAndDeploymentID(
                calendar,
                deploymentIdArrayInt
            )
            this@EdgeDeploymentActivity.runOnUiThread {
                startSyncing(SyncFragment.AFTER_SYNC)
            }
        }.start()
    }

    override fun playTone() {
        Thread {
            audioMothConnector.playTone(
                5000
            )
        }.start()
    }

    override fun startMapPicker(latitude: Double, longitude: Double, name: String) {
        setLatLng(latitude, longitude, name)
        startFragment(MapPickerFragment.newInstance(latitude, longitude, name))
    }

    private fun handleDeploymentStep(deploymentId: Int) {
        val deployment = deploymentDb.getDeploymentById(deploymentId)
        if (deployment != null) {
            setDeployment(deployment)

            if (deployment.location != null) {
                _deployLocation = deployment.location
            }

            currentCheck = if (deployment.state == 1) {
                deployment.state
            } else {
                deployment.state - 1
            }
            handleCheckClicked(currentCheck)
        }
    }

    private fun startFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(contentContainer.id, fragment)
            .commit()
    }

    private fun updateDeploymentState(state: DeploymentState.Edge) {
        this._deployment?.state = state.key
        this._deployment?.let { deploymentDb.updateDeployment(it) }
    }

    private fun showLoading() {
        val loadingDialog: LoadingDialogFragment =
            supportFragmentManager.findFragmentByTag(loadingDialogTag) as LoadingDialogFragment?
                ?: run {
                    LoadingDialogFragment()
                }
        loadingDialog.show(supportFragmentManager, loadingDialogTag)
    }

    private fun showComplete() {
        val completeFragment: CompleteFragment =
            supportFragmentManager.findFragmentByTag(CompleteFragment.tag) as CompleteFragment?
                ?: run {
                    CompleteFragment()
                }
        completeFragment.isCancelable = false
        completeFragment.show(supportFragmentManager, CompleteFragment.tag)
    }

    private fun hideLoading() {
        val loadingDialog: LoadingDialogFragment? =
            supportFragmentManager.findFragmentByTag(loadingDialogTag) as LoadingDialogFragment?
        loadingDialog?.dismissDialog()
    }

    override fun onAnimationEnd() {
        finish()
    }

    override fun onBackPressed() {
        backStep()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        val fragment =
            supportFragmentManager.findFragmentByTag(LocationFragment.TAG) as LocationFragment?
                ?: LocationFragment.newInstance()
        fragment.onActivityResult(requestCode, resultCode, data)
    }

    private fun handleNestedFragmentBackStack(fragmentManager: FragmentManager): Boolean {
        val childFragmentList = fragmentManager.fragments
        if (childFragmentList.size > 0) {
            for (index in childFragmentList.size - 1 downTo 0) {
                val fragment = childFragmentList[index]
                val isPopped = handleNestedFragmentBackStack(fragment.childFragmentManager)
                return when {
                    isPopped -> true
                    fragmentManager.backStackEntryCount > 0 -> {
                        fragmentManager.popBackStack()
                        true
                    }
                    else -> false
                }
            }
        }
        return false
    }

    companion object {
        const val loadingDialogTag = "LoadingDialog"
        const val EXTRA_DEPLOYMENT_ID = "EXTRA_DEPLOYMENT_ID"

        fun startActivity(context: Context) {
            val intent = Intent(context, EdgeDeploymentActivity::class.java)
            context.startActivity(intent)
        }

        fun startActivity(context: Context, deploymentId: Int) {
            val intent = Intent(context, EdgeDeploymentActivity::class.java)
            intent.putExtra(EXTRA_DEPLOYMENT_ID, deploymentId)
            context.startActivity(intent)
        }

        fun startActivity(context: Context, deploymentId: Int, requestCode: Int) {
            val intent = Intent(context, EdgeDeploymentActivity::class.java)
            intent.putExtra(EXTRA_DEPLOYMENT_ID, deploymentId)
            (context as Activity).startActivityForResult(intent, requestCode)
        }
    }
}
