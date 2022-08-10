package org.rfcx.companion.view.profile.guardianregistration

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
import kotlinx.android.synthetic.main.activity_guardian_registration.*
import kotlinx.android.synthetic.main.toolbar_default.*
import org.rfcx.companion.R
import org.rfcx.companion.base.ViewModelFactory
import org.rfcx.companion.entity.RegisterGuardian
import org.rfcx.companion.entity.guardian.GuardianRegistration
import org.rfcx.companion.repo.api.CoreApiHelper
import org.rfcx.companion.repo.api.CoreApiServiceImpl
import org.rfcx.companion.repo.api.DeviceApiHelper
import org.rfcx.companion.repo.api.DeviceApiServiceImpl
import org.rfcx.companion.repo.local.LocalDataHelper
import org.rfcx.companion.service.RegisterGuardianWorker
import org.rfcx.companion.util.isNetworkAvailable
import org.rfcx.companion.view.map.SyncInfo

class RegisterGuardianActivity : AppCompatActivity(), RegisterGuardianListener {

    private val registerAdapter by lazy { RegisterGuardianAdapter(this) }

    private lateinit var viewModel: RegisterGuardianViewModel

    private lateinit var registerWorkInfoLiveData: LiveData<List<WorkInfo>>

    private var lastSyncingInfo: SyncInfo? = null
    private var currentState: WorkInfo.State? = null
    private var register: List<GuardianRegistration>? = null

    private val registerWorkInfoObserve = Observer<List<WorkInfo>> {
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
        setContentView(R.layout.activity_guardian_registration)

        setViewModel()
        setObserve()

        setupToolbar()

        registerGuardianRecyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = registerAdapter
        }

        confirmButton.setOnClickListener {
            viewModel.registerGuardians()
            it.isEnabled = false
        }
    }

    private fun showBanner() {
        TransitionManager.beginDelayedTransition(banner, RegisterBannerTransition())
        banner.visibility = View.VISIBLE
    }

    private fun hideBanner() {
        TransitionManager.beginDelayedTransition(banner, RegisterBannerTransition())
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
        ).get(RegisterGuardianViewModel::class.java)
    }

    private fun setObserve() {
        registerWorkInfoLiveData = RegisterGuardianWorker.workInfos(this)
        registerWorkInfoLiveData.observeForever(registerWorkInfoObserve)

        viewModel.getRegistrations().observe(
            this
        ) {
            setRegisterText(it.size)
            register = it

            val errors = RegisterGuardianWorker.getErrors()
            registerAdapter.items = it.map { rg ->
                val error = errors.find { error -> error.guid == rg.guid }
                RegisterGuardian(
                    rg.guid,
                    error?.error
                )
            }
        }
    }

    private fun setupToolbar() {
        setSupportActionBar(toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowHomeEnabled(true)
            title = getString(R.string.guardian_registration)
        }
    }

    private fun updateSyncInfo(syncInfo: SyncInfo? = null) {
        val status = syncInfo
            ?: if (this.isNetworkAvailable()) SyncInfo.Starting else SyncInfo.WaitingNetwork
        if (this.lastSyncingInfo == SyncInfo.Uploaded && status == SyncInfo.Uploaded) return
        this.lastSyncingInfo = status
        setStatus(status)
    }

    private fun setRegisterText(count: Int) {
        when {
            count == 0 -> {
                bannerText.text = getString(R.string.s_register_guardian)
                registerAdapter.items = listOf()
                noContentTextView.visibility = View.VISIBLE
                hideBanner()
            }
            currentState == WorkInfo.State.RUNNING -> {
                bannerText.text = getString(R.string.s_register_guardian_text, count)
                noContentTextView.visibility = View.GONE
            }
            count > 0 && currentState != WorkInfo.State.RUNNING -> {
                bannerText.text = getString(R.string.s_register_guardian_text, count)
                noContentTextView.visibility = View.GONE
                showBanner()
            }
        }
    }

    private fun setStatus(status: SyncInfo) {
        when (status) {
            SyncInfo.Starting, SyncInfo.Uploading -> {
                confirmButton.text = getString(R.string.s_registering)
                confirmButton.isEnabled = false
                registerGuardianIndicator.visibility = View.VISIBLE
            }
            SyncInfo.Uploaded -> {
                confirmButton.text = getString(R.string.s_registered)
                confirmButton.isEnabled = true
                registerGuardianIndicator.visibility = View.GONE
                hideBanner()
            }
            SyncInfo.Failed, SyncInfo.Retry -> {
                confirmButton.text = getString(R.string.register)
                confirmButton.isEnabled = true
                registerGuardianIndicator.visibility = View.GONE
                showBanner()
                val errors = RegisterGuardianWorker.getErrors()
                register?.map { rg ->
                    val error = errors.find { error -> error.guid == rg.guid }
                    RegisterGuardian(
                        rg.guid,
                        error?.error
                    )
                }?.let {
                    registerAdapter.items = it
                }
            }
            // else also waiting network
            else -> {
                Toast.makeText(
                    this,
                    getString(R.string.s_guardian_registered_no_conntection),
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    override fun onClick(guid: String) {
        viewModel.deleteRegistration(guid)
        val updatedItems = registerAdapter.items.filter { it.guid != guid }
        registerAdapter.items = updatedItems
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
            val intent = Intent(context, RegisterGuardianActivity::class.java)
            context.startActivity(intent)
        }
    }
}
