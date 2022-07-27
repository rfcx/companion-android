package org.rfcx.companion.view.profile.classifier

import android.content.Context
import android.content.Intent
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
    private val classifierAdapter by lazy { GuardianClassifierAdapter(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_guardian_classifier)

        setViewModel()
        setupToolbar()
        setObserver()

        classifierRecyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = classifierAdapter
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
                        classifierAdapter.availableClassifiers = classifiers
                        classifierAdapter.downloadedClassifiers =
                            guardianClassifierViewModel.getDownloadedClassifiers() ?: listOf()
                    }
                }
                Status.ERROR -> {
                    hideLoading()
                    showToast(res.message ?: getString(R.string.error_has_occurred))
                }
            }
        }

        guardianClassifierViewModel.getDownloadClassifier().observe(this) { res ->
            when (res.status) {
                Status.LOADING -> {}
                Status.SUCCESS -> {
                    classifierAdapter.needLoading = false
                    classifierAdapter.selected = -1
                    classifierAdapter.notifyDataSetChanged()
                    res.data?.let { classifier ->
                        guardianClassifierViewModel.saveClassifier(classifier)
                    }
                }
                Status.ERROR -> {
                    classifierAdapter.needLoading = false
                    classifierAdapter.selected = -1
                    classifierAdapter.notifyDataSetChanged()
                    showToast(res.message ?: getString(R.string.error_has_occurred))
                }
            }
        }

        guardianClassifierViewModel.getDownloadedClassifiersLiveData().observe(this) { res ->
            classifierAdapter.downloadedClassifiers = res
            guardianClassifierViewModel.reCompareDownloadedClassifiersWithCacheResponse()
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
        guardianClassifierViewModel.downloadClassifier(classifier)
    }

    override fun onDeleteClicked(classifier: GuardianClassifierResponse) {
        guardianClassifierViewModel.deleteClassifier(classifier.id)
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }

    companion object {

        fun startActivity(context: Context) {
            val intent = Intent(context, GuardianClassifierActivity::class.java)
            context.startActivity(intent)
        }
    }
}
