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

        adminDownloadButton.setOnClickListener {
            guardianSoftwareViewModel.downloadSoftware(ADMIN)
        }
        classifyDownloadButton.setOnClickListener {
            guardianSoftwareViewModel.downloadSoftware(CLASSIFY)
        }
        guardianDownloadButton.setOnClickListener {
            guardianSoftwareViewModel.downloadSoftware(GUARDIAN)
        }
        updaterDownloadButton.setOnClickListener {
            guardianSoftwareViewModel.downloadSoftware(UPDATER)
        }
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
                                    ADMIN -> {
                                        if (it.value.status != APKUtils.APKStatus.UP_TO_DATE) {
                                            adminDownloadButton.visibility = View.VISIBLE
                                            adminDownloadButton.text = "${adminDownloadButton.text} ${it.value.version}"
                                        } else {
                                            adminStatus.visibility = View.VISIBLE
                                        }
                                    }
                                    CLASSIFY -> {
                                        if (it.value.status != APKUtils.APKStatus.UP_TO_DATE) {
                                            classifyDownloadButton.visibility = View.VISIBLE
                                            classifyDownloadButton.text = "${classifyDownloadButton.text} ${it.value.version}"
                                        } else {
                                            classifyStatus.visibility = View.VISIBLE
                                        }
                                    }
                                    GUARDIAN -> {
                                        if (it.value.status != APKUtils.APKStatus.UP_TO_DATE) {
                                            guardianDownloadButton.visibility = View.VISIBLE
                                            guardianDownloadButton.text = "${guardianDownloadButton.text} ${it.value.version}"
                                        } else {
                                            guardianStatus.visibility = View.VISIBLE
                                        }
                                    }
                                    UPDATER -> {
                                        if (it.value.status != APKUtils.APKStatus.UP_TO_DATE) {
                                            updaterDownloadButton.visibility = View.VISIBLE
                                            updaterDownloadButton.text = "${updaterDownloadButton.text} ${it.value.version}"
                                        } else {
                                            updaterStatus.visibility = View.VISIBLE
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

        guardianSoftwareViewModel.getSoftwareFileDownload().observe(this, Observer { downloadStatus ->
            when (downloadStatus.status) {
                Status.LOADING -> {
                    downloadStatus.data?.let { role ->
                        showDownloadingLoading(role)
                    }
                }
                Status.SUCCESS -> {
                    downloadStatus.data?.let { role ->
                        hideDownloadingLoading(role)
                        when(role) {
                            ADMIN -> {
                                adminDownloadButton.visibility = View.GONE
                                adminStatus.visibility = View.VISIBLE
                            }
                            CLASSIFY -> {
                                classifyDownloadButton.visibility = View.GONE
                                classifyStatus.visibility = View.VISIBLE
                            }
                            GUARDIAN -> {
                                guardianDownloadButton.visibility = View.GONE
                                guardianStatus.visibility = View.VISIBLE
                            }
                            UPDATER -> {
                                updaterDownloadButton.visibility = View.GONE
                                updaterStatus.visibility = View.VISIBLE
                            }
                        }
                        setView()
                    }
                }
                Status.ERROR -> {
                    hideLoading()
                    showToast(downloadStatus.message ?: getString(R.string.error_has_occurred))
                }
            }
        })
    }

    private fun setView() {
        val versions = guardianSoftwareViewModel.getCurrentDownloadedAPKsVersions()
        versions?.forEach {
            when(it.key) {
                ADMIN -> adminRoleVersion.text = "${it.value.first} (local)"
                CLASSIFY -> classifyRoleVersion.text = "${it.value.first} (local)"
                GUARDIAN -> guardianRoleVersion.text = "${it.value.first} (local)"
                UPDATER -> updaterRoleVersion.text = "${it.value.first} (local)"
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

    private fun showDownloadingLoading(role: String) {
        when(role) {
            ADMIN -> {
                adminLoading.visibility = View.VISIBLE
                adminDownloadButton.visibility = View.GONE
            }
            CLASSIFY -> {
                classifyLoading.visibility = View.VISIBLE
                classifyDownloadButton.visibility = View.GONE
            }
            GUARDIAN -> {
                guardianLoading.visibility = View.VISIBLE
                guardianDownloadButton.visibility = View.GONE
            }
            UPDATER -> {
                updaterLoading.visibility = View.VISIBLE
                updaterDownloadButton.visibility = View.GONE
            }
        }
    }

    private fun hideDownloadingLoading(role: String) {
        when(role) {
            ADMIN -> {
                adminLoading.visibility = View.GONE
                adminDownloadButton.visibility = View.VISIBLE
            }
            CLASSIFY -> {
                classifyLoading.visibility = View.GONE
                classifyDownloadButton.visibility = View.VISIBLE
            }
            GUARDIAN -> {
                guardianLoading.visibility = View.GONE
                guardianDownloadButton.visibility = View.VISIBLE
            }
            UPDATER -> {
                updaterLoading.visibility = View.GONE
                updaterDownloadButton.visibility = View.VISIBLE
            }
        }
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }

    override fun onBackPressed() {
        finish()
    }

    companion object {

        private const val ADMIN = "admin"
        private const val CLASSIFY = "classify"
        private const val GUARDIAN = "guardian"
        private const val UPDATER = "updater"

        fun startActivity(context: Context) {
            val intent = Intent(context, GuardianSoftwareActivity::class.java)
            context.startActivity(intent)
        }
    }
}