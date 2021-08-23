package org.rfcx.companion.view.profile.guardiansoftware

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import kotlinx.android.synthetic.main.activity_guardian_software.*
import kotlinx.android.synthetic.main.toolbar_default.*
import org.rfcx.companion.R
import org.rfcx.companion.base.ViewModelFactory
import org.rfcx.companion.repo.api.CoreApiHelper
import org.rfcx.companion.repo.api.CoreApiServiceImpl
import org.rfcx.companion.repo.api.DeviceApiHelper
import org.rfcx.companion.repo.api.DeviceApiServiceImpl
import org.rfcx.companion.repo.local.LocalDataHelper
import org.rfcx.companion.util.Status
import org.rfcx.companion.util.file.APKUtils
import org.rfcx.companion.util.isNetworkAvailable
import org.rfcx.companion.view.profile.guardiansoftware.viewmodel.GuardianSoftwareViewModel

class GuardianSoftwareActivity : AppCompatActivity() {

    private lateinit var guardianSoftwareViewModel: GuardianSoftwareViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_guardian_software)
        setupToolbar()
        setViewModel()

        if (this.isNetworkAvailable()) {
            setObserver()
        } else {
            showToast(getString(R.string.network_not_available))
        }

        setView()
    }

    private fun setViewModel() {
        guardianSoftwareViewModel = ViewModelProvider(
            this,
            ViewModelFactory(
                application,
                DeviceApiHelper(DeviceApiServiceImpl()),
                CoreApiHelper(CoreApiServiceImpl()),
                LocalDataHelper()
            )
        ).get(GuardianSoftwareViewModel::class.java)
    }

    private fun setupToolbar() {
        setSupportActionBar(toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowHomeEnabled(true)
            title = getString(R.string.guardian_softwares)
        }
    }

    private fun setObserver() {
        guardianSoftwareViewModel.getSoftwareVersion().observe(this, Observer { roleStatuses ->
            when (roleStatuses.status) {
                Status.LOADING -> {
                    showLoading()
                }
                Status.SUCCESS -> {
                    hideLoading()
                    roleStatuses.data?.let { roleStatus ->
                        if (roleStatus.isNotEmpty()) {
                            roleStatus.forEach {
                                when (it.key) {
                                    "admin" -> {
                                        if (it.value != APKUtils.APKStatus.UP_TO_DATE) {
                                            adminDownloadButton.visibility = View.VISIBLE
                                        } else {
                                            adminDownloadButton.visibility = View.GONE
                                        }
                                    }
                                    "classify" -> {
                                        if (it.value != APKUtils.APKStatus.UP_TO_DATE) {
                                            classifyDownloadButton.visibility = View.VISIBLE
                                        } else {
                                            classifyDownloadButton.visibility = View.GONE
                                        }
                                    }
                                    "guardian" -> {
                                        if (it.value != APKUtils.APKStatus.UP_TO_DATE) {
                                            guardianDownloadButton.visibility = View.VISIBLE
                                        } else {
                                            guardianDownloadButton.visibility = View.GONE
                                        }
                                    }
                                    "updater" -> {
                                        if (it.value != APKUtils.APKStatus.UP_TO_DATE) {
                                            updaterDownloadButton.visibility = View.VISIBLE
                                        } else {
                                            updaterDownloadButton.visibility = View.GONE
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                Status.ERROR -> {
                    hideLoading()
                    showToast(roleStatuses.message ?: getString(R.string.error_has_occurred))
                }
            }
        })
    }

    private fun setView() {
        val versions = guardianSoftwareViewModel.getCurrentDownloadedAPKsVersions()
        versions.forEach {
            when(it.key) {
                "admin" -> adminRoleVersion.text = it.value
                "classify" -> classifyRoleVersion.text = it.value
                "guardian" -> guardianRoleVersion.text = it.value
                "updater" -> updaterRoleVersion.text = it.value
            }
        }
    }

    private fun showLoading() {
        softwareLoading.visibility = View.VISIBLE
        roleLayout.visibility = View.GONE
    }

    private fun hideLoading() {
        softwareLoading.visibility = View.GONE
        roleLayout.visibility = View.VISIBLE
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }

    companion object {
        fun startActivity(context: Context) {
            val intent = Intent(context, GuardianSoftwareActivity::class.java)
            context.startActivity(intent)
        }
    }
}
