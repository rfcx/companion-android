package org.rfcx.companion.view.profile.guardianclassifier

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.android.synthetic.main.activity_guardian_classifier.*
import kotlinx.android.synthetic.main.toolbar_default.*
import org.rfcx.companion.R
import org.rfcx.companion.base.ViewModelFactory
import org.rfcx.companion.entity.socket.Classifier
import org.rfcx.companion.repo.api.CoreApiHelper
import org.rfcx.companion.repo.api.CoreApiServiceImpl
import org.rfcx.companion.repo.api.DeviceApiHelper
import org.rfcx.companion.repo.api.DeviceApiServiceImpl
import org.rfcx.companion.repo.local.LocalDataHelper
import org.rfcx.companion.util.getIdToken
import org.rfcx.companion.util.isNetworkAvailable
import org.rfcx.companion.view.profile.guardianclassifier.viewmodel.GuardianClassifierViewModel

class GuardianClassifierActivity : AppCompatActivity(), ClassifierDownloadListener {

    private lateinit var guardianClassifierViewModel: GuardianClassifierViewModel
    private var classifierAdapter: GuardianClassifierAdapter? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_guardian_classifier)
        setViewModel()
        setupToolbar()

        if (this.isNetworkAvailable()) {
            setObserver()
        } else {
            showToast(getString(R.string.network_not_available))
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
            title = getString(R.string.guardian_classifier)
        }
    }

    private fun setObserver() {
        val token = this.getIdToken() ?: return
        guardianClassifierViewModel.checkClassifiers(token)
        //TODO: add check classifier here
    }

    private fun populateAdapterWithInfo(classifiers: List<Classifier>) {
        classifierAdapter = GuardianClassifierAdapter(this)
        classifierAdapter?.let {
            val layoutManager = LinearLayoutManager(this)
            classifierRecyclerView.layoutManager = layoutManager
            classifierRecyclerView.adapter = it
            classifierRecyclerView.addItemDecoration(
                DividerItemDecoration(
                    this,
                    DividerItemDecoration.VERTICAL
                )
            )
            it.items = classifiers
            it.notifyDataSetChanged()
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }

    override fun onBackPressed() {
        finish()
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }

    companion object {
        fun startActivity(context: Context) {
            val intent = Intent(context, GuardianClassifierActivity::class.java)
            context.startActivity(intent)
        }
    }

    override fun onDownloadClicked(path: String) {
        //TODO: add call download here
    }
}
