package org.rfcx.companion.view.profile.classifier

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.android.synthetic.main.activity_guardian_classifier.*
import kotlinx.android.synthetic.main.fragment_classifier.classifierRecyclerView
import kotlinx.android.synthetic.main.toolbar_default.*
import org.rfcx.companion.R
import org.rfcx.companion.base.ViewModelFactory
import org.rfcx.companion.entity.response.GuardianClassifierResponse
import org.rfcx.companion.repo.api.CoreApiHelper
import org.rfcx.companion.repo.api.CoreApiServiceImpl
import org.rfcx.companion.repo.api.DeviceApiHelper
import org.rfcx.companion.repo.api.DeviceApiServiceImpl
import org.rfcx.companion.repo.local.LocalDataHelper
import org.rfcx.companion.util.Status
import org.rfcx.companion.view.profile.classifier.viewmodel.GuardianClassifierViewModel

class GuardianClassifierActivity : AppCompatActivity(), ClassifierListener {

    private lateinit var guardianClassifierViewModel: GuardianClassifierViewModel
    private val adapter by lazy { GuardianClassifierAdapter(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_guardian_classifier)

        setViewModel()
        setupToolbar()
        setObserver()

        classifierRecyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = adapter
        }
    }

    private fun setViewModel() {
        guardianClassifierViewModel = ViewModelProvider(
            this,
            ViewModelFactory(
                application,
                DeviceApiHelper(DeviceApiServiceImpl()),
                CoreApiHelper(CoreApiServiceImpl()),
                LocalDataHelper()
            )
        ).get(GuardianClassifierViewModel::class.java)
    }

    private fun setupToolbar() {
        setSupportActionBar(toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowHomeEnabled(true)
            title = getString(R.string.classifier)
        }
    }

    private fun setObserver() {
        guardianClassifierViewModel.getAvailableClassifiers().observe(this) { res ->
            when (res.status) {
                Status.LOADING -> {
                    showLoading()
                }
                Status.SUCCESS -> {
                    hideLoading()
                    res.data?.let { classifiers ->
                        adapter.availableClassifiers = classifiers
                        adapter.downloadedClassifiers = guardianClassifierViewModel.getDownloadedClassifiers() ?: listOf()
                    }
                }
                Status.ERROR -> {
                    hideLoading()
                    showToast(res.message ?: getString(R.string.error_has_occurred))
                }
            }
        }
    }

    private fun showLoading() {
        classifierLoading.visibility = View.VISIBLE
        classifierRecyclerView.visibility = View.GONE
    }

    private fun hideLoading() {
        classifierLoading.visibility = View.GONE
        classifierRecyclerView.visibility = View.VISIBLE
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }

    override fun onDownloadClicked(classifier: GuardianClassifierResponse) {
        TODO("Not yet implemented")
    }

    override fun onDeleteClicked(classifier: GuardianClassifierResponse) {
        TODO("Not yet implemented")
    }

}
