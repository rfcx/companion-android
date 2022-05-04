package org.rfcx.companion.view.unsynced

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.transition.TransitionManager
import androidx.work.WorkInfo
import kotlinx.android.synthetic.main.activity_unsynced_deployment.*
import kotlinx.android.synthetic.main.toolbar_default.*
import org.rfcx.companion.R
import org.rfcx.companion.base.ViewModelFactory
import org.rfcx.companion.repo.api.CoreApiHelper
import org.rfcx.companion.repo.api.CoreApiServiceImpl
import org.rfcx.companion.repo.api.DeviceApiHelper
import org.rfcx.companion.repo.api.DeviceApiServiceImpl
import org.rfcx.companion.repo.local.LocalDataHelper
import org.rfcx.companion.service.DeploymentSyncWorker
import org.rfcx.companion.util.Status
import org.rfcx.companion.util.isNetworkAvailable
import org.rfcx.companion.view.map.SyncInfo


class UnsyncedDeploymentActivity : AppCompatActivity(), UnsyncedDeploymentListener {

    private val unsyncedAdapter by lazy { UnsyncedDeploymentAdapter(this) }

    private lateinit var viewModel: UnsyncedDeploymentViewModel

    private lateinit var deploymentWorkInfoLiveData: LiveData<List<WorkInfo>>

    private var lastSyncingInfo: SyncInfo? = null
    private var currentState: WorkInfo.State? = null

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
        setContentView(R.layout.activity_unsynced_deployment)

        setViewModel()
        setObserve()

        setupToolbar()

        unsyncedRecyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = unsyncedAdapter
            val decoration = DividerItemDecoration(context, DividerItemDecoration.VERTICAL)
            decoration.setDrawable(ContextCompat.getDrawable(context, R.drawable.divider)!!)
            this.addItemDecoration(decoration)
        }

        dismissButton.setOnClickListener {
            hideBanner()
        }

        confirmButton.setOnClickListener {
            viewModel.syncDeployment()
        }

    }

    private fun showBanner() {
        TransitionManager.beginDelayedTransition(rootView, UnsyncedBannerTransition())
        banner.visibility = View.VISIBLE
    }

    private fun hideBanner() {
        TransitionManager.beginDelayedTransition(rootView, UnsyncedBannerTransition())
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
        ).get(UnsyncedDeploymentViewModel::class.java)
    }

    private fun setObserve() {
        deploymentWorkInfoLiveData = DeploymentSyncWorker.workInfos(this)
        deploymentWorkInfoLiveData.observeForever(deploymentWorkInfoObserve)

        viewModel.getUnsyncedDeployments().observe(
            this
        ) {
            when (it.status) {
                Status.LOADING -> {}
                Status.SUCCESS -> {
                    setUnsyncedText(it.data ?: 0)
                }
                Status.ERROR -> {}
            }
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

    private fun setUnsyncedText(count: Int) {
        when {
            count == 0 -> {
                bannerText.text = getString(R.string.format_deploys_uploaded)
                confirmButton.isEnabled = false
                noContentTextView.visibility = View.VISIBLE
                hideBanner()
            }
            currentState == WorkInfo.State.RUNNING -> {
                bannerText.text = getString(R.string.unsynced_text, count)
                noContentTextView.visibility = View.GONE
            }
            count > 0 && currentState != WorkInfo.State.RUNNING -> {
                bannerText.text = getString(R.string.unsynced_text, count)
                confirmButton.isEnabled = true
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
            }
            SyncInfo.Uploaded -> {
                confirmButton.text = getString(R.string.sync)
                confirmButton.isEnabled = true
            }
            SyncInfo.Failed, SyncInfo.Retry -> {
                confirmButton.text = getString(R.string.sync)
                confirmButton.isEnabled = true
                val errors = DeploymentSyncWorker.getErrors()
                if (errors.isNotEmpty()) {
                    unsyncedAdapter.items = errors
                    unsyncedFailedLayout.visibility = View.VISIBLE
                } else {
                    unsyncedFailedLayout.visibility = View.GONE
                }
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

    override fun onClick(id: Int) {
        viewModel.deleteDeployment(id)
        val updatedItems = unsyncedAdapter.items.filter { it.id != id }
        unsyncedAdapter.items = updatedItems
        if (updatedItems.isEmpty()) {
            unsyncedFailedLayout.visibility = View.GONE
        }
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
            val intent = Intent(context, UnsyncedDeploymentActivity::class.java)
            context.startActivity(intent)
        }
    }
}
