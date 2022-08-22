package org.rfcx.companion.view.unsynced

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.transition.TransitionManager
import androidx.work.WorkInfo
import kotlinx.android.synthetic.main.activity_unsynced_works.*
import kotlinx.android.synthetic.main.toolbar_default.*
import org.rfcx.companion.R
import org.rfcx.companion.adapter.UnsyncedWorksViewItem
import org.rfcx.companion.base.ViewModelFactory
import org.rfcx.companion.repo.api.CoreApiHelper
import org.rfcx.companion.repo.api.CoreApiServiceImpl
import org.rfcx.companion.repo.api.DeviceApiHelper
import org.rfcx.companion.repo.api.DeviceApiServiceImpl
import org.rfcx.companion.repo.local.LocalDataHelper
import org.rfcx.companion.service.DeploymentSyncWorker
import org.rfcx.companion.util.isNetworkAvailable
import org.rfcx.companion.view.map.SyncInfo

class UnsyncedWorksActivity : AppCompatActivity(), UnsyncedWorkListener {

    private val unsyncedWorksAdapter by lazy { UnsyncedWorksAdapter(this) }

    private lateinit var viewModel: UnsyncedWorksViewModel

    private lateinit var deploymentWorkInfoLiveData: LiveData<List<WorkInfo>>
    private lateinit var registrationWorkInfoLiveData: LiveData<List<WorkInfo>>

    private var lastSyncingInfo: SyncInfo? = null
    private var currentState: WorkInfo.State? = null
    private var unsyncedWork: List<UnsyncedWorksViewItem>? = null

    private val deploymentWorkInfoObserve = Observer<List<WorkInfo>> {
        val currentWorkStatus = it?.getOrNull(0)
        if (currentWorkStatus != null) {
            currentState = currentWorkStatus.state
            when (currentWorkStatus.state) {
                WorkInfo.State.RUNNING -> updateSyncInfo(SyncInfo.Uploading)
                WorkInfo.State.SUCCEEDED -> updateSyncInfo(SyncInfo.Uploaded)
                WorkInfo.State.FAILED -> updateSyncInfo(SyncInfo.Failed)
                else -> {
                    if (currentWorkStatus.runAttemptCount >= 1) {
                        updateSyncInfo(SyncInfo.Retry)
                    } else {
                        updateSyncInfo()
                    }
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_unsynced_works)

        setViewModel()
        setObserve()

        setupToolbar()

        unsyncedRecyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = unsyncedWorksAdapter
        }

        confirmButton.setOnClickListener {
            viewModel.syncDeployment()
            it.isEnabled = false
        }
    }

    private fun showBanner() {
        TransitionManager.beginDelayedTransition(banner, UnsyncedBannerTransition())
        banner.visibility = View.VISIBLE
    }

    private fun hideBanner() {
        TransitionManager.beginDelayedTransition(banner, UnsyncedBannerTransition())
        banner.visibility = View.GONE
    }

    private fun setViewModel() {
        viewModel = ViewModelProvider(
            this,
            ViewModelFactory(
                application,
                DeviceApiHelper(DeviceApiServiceImpl()),
                CoreApiHelper(CoreApiServiceImpl()),
                LocalDataHelper()
            )
        ).get(UnsyncedWorksViewModel::class.java)
    }

    private fun setObserve() {
        deploymentWorkInfoLiveData = DeploymentSyncWorker.workInfos(this)
        deploymentWorkInfoLiveData.observeForever(deploymentWorkInfoObserve)

        viewModel.getUnsyncedWorkLiveData().observe(
            this
        ) {
            setUnsyncedText(it.count { item -> item is UnsyncedWorksViewItem.Deployment }, it.count { item -> item is UnsyncedWorksViewItem.Registration })
            unsyncedWork = it
            unsyncedWorksAdapter.setUnsynceds(it)
        }
    }

    private fun setupToolbar() {
        setSupportActionBar(toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowHomeEnabled(true)
            title = getString(R.string.unsynced_deployments)
        }
    }

    private fun updateSyncInfo(syncInfo: SyncInfo? = null) {
        val status = syncInfo
            ?: if (this.isNetworkAvailable()) SyncInfo.Starting else SyncInfo.WaitingNetwork
        if (this.lastSyncingInfo == SyncInfo.Uploaded && status == SyncInfo.Uploaded) return
        this.lastSyncingInfo = status
        setStatus(status)
    }

    private fun setUnsyncedText(deploymentCount: Int, registrationCount: Int) {
        when {
            deploymentCount == 0 && registrationCount == 0 -> {
                bannerText.text = getString(R.string.all_works_synced)
                unsyncedWorksAdapter.setUnsynceds(listOf())
                noContentTextView.visibility = View.VISIBLE
                hideBanner()
            }
            else -> {
                when {
                    deploymentCount == 0 -> {
                        bannerText.text = getString(R.string.unsynced_registration_text, registrationCount)
                    }
                    registrationCount == 0 -> {
                        bannerText.text = getString(R.string.unsynced_deployment_text, deploymentCount)
                    }
                    else -> {
                        bannerText.text = getString(R.string.unsynced_all_text, deploymentCount, registrationCount)
                    }
                }
                noContentTextView.visibility = View.GONE
                showBanner()
            }
        }
    }

    private fun setStatus(status: SyncInfo) {
        when (status) {
            SyncInfo.Starting, SyncInfo.Uploading -> {
                confirmButton.text = getString(R.string.syncing)
                confirmButton.isEnabled = false
                unsyncedIndicator.visibility = View.VISIBLE
            }
            SyncInfo.Uploaded -> {
                confirmButton.text = getString(R.string.sync)
                confirmButton.isEnabled = true
                unsyncedIndicator.visibility = View.GONE
                hideBanner()
            }
            SyncInfo.Failed, SyncInfo.Retry -> {
                confirmButton.text = getString(R.string.sync)
                confirmButton.isEnabled = true
                unsyncedIndicator.visibility = View.GONE
                showBanner()
                viewModel.updateUnsyncedWorks()
            }
            // else also waiting network
            else -> {
                Toast.makeText(
                    this,
                    getString(R.string.format_deploy_waiting_network),
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    override fun onDeploymentClick(id: Int) {
        viewModel.deleteDeployment(id)
    }

    override fun onRegistrationClick(id: String) {
        viewModel.deleteRegistration(id)
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }

    override fun onBackPressed() {
        finish()
    }

    companion object {
        fun startActivity(context: Context) {
            val intent = Intent(context, UnsyncedWorksActivity::class.java)
            context.startActivity(intent)
        }
    }
}
