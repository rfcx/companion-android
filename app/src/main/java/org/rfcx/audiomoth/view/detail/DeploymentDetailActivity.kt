package org.rfcx.audiomoth.view.detail

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.TypedValue
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.Transformations
import androidx.recyclerview.widget.LinearLayoutManager
import io.realm.Realm
import kotlinx.android.synthetic.main.activity_deployment_detail.*
import kotlinx.android.synthetic.main.toolbar_default.*
import org.rfcx.audiomoth.R
import org.rfcx.audiomoth.entity.DeploymentImage
import org.rfcx.audiomoth.entity.EdgeDeployment
import org.rfcx.audiomoth.entity.Screen
import org.rfcx.audiomoth.localdb.DatabaseCallback
import org.rfcx.audiomoth.localdb.DeploymentImageDb
import org.rfcx.audiomoth.localdb.EdgeDeploymentDb
import org.rfcx.audiomoth.service.DeploymentSyncWorker
import org.rfcx.audiomoth.util.RealmHelper
import org.rfcx.audiomoth.util.asLiveData
import org.rfcx.audiomoth.util.convertLatLngLabel
import org.rfcx.audiomoth.util.showCommonDialog
import org.rfcx.audiomoth.view.BaseActivity
import org.rfcx.audiomoth.view.deployment.EdgeDeploymentActivity.Companion.EXTRA_DEPLOYMENT_ID
import org.rfcx.audiomoth.view.profile.locationgroup.LocationGroupActivity

class DeploymentDetailActivity : BaseActivity() {
    private val realm by lazy { Realm.getInstance(RealmHelper.migrationConfig()) }
    private val edgeDeploymentDb by lazy { EdgeDeploymentDb(realm) }
    private val deploymentImageDb by lazy { DeploymentImageDb(realm) }
    private val deploymentImageAdapter by lazy { DeploymentImageAdapter() }

    // data
    private var deployment: EdgeDeployment? = null
    private lateinit var deployImageLiveData: LiveData<List<DeploymentImage>>
    private var deploymentImages = listOf<DeploymentImage>()
    private val deploymentImageObserve = Observer<List<DeploymentImage>> {
        deploymentImages = it
        updateDeploymentImages(deploymentImages)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_deployment_detail)

        deployment =
            intent.extras?.getInt(EXTRA_DEPLOYMENT_ID)
                ?.let { edgeDeploymentDb.getDeploymentById(it) }

        setupToolbar()
        setupImageRecycler()
        deployment?.let { updateDeploymentDetailView(it) }

        // setup onclick
        deleteButton.setOnClickListener {
            confirmationDialog()
        }

        editButton.setOnClickListener {
            deployment?.let {
                val location = deployment?.location
                location?.let { locate ->
                    intent.extras?.getInt(EXTRA_DEPLOYMENT_ID)?.let { deploymentId ->
                        EditLocationActivity.startActivity(
                            this,
                            locate.latitude,
                            locate.longitude,
                            locate.name,
                            deploymentId,
                            locationGroupValueTextView.text.toString(),
                            DEPLOYMENT_REQUEST_CODE
                        )
                    }
                }
            }
        }

        editGroupButton.setOnClickListener {
            val group = locationGroupValueTextView.text.toString()
            val setLocationGroup = if (group == getString(R.string.none)) null else group
            intent.extras?.getInt(EXTRA_DEPLOYMENT_ID)?.let { deploymentId ->
                LocationGroupActivity.startActivity(
                    this,
                    setLocationGroup,
                    deploymentId,
                    Screen.EDGE_DETAIL.id,
                    DEPLOYMENT_REQUEST_CODE
                )
            }
        }
    }

    private fun confirmationDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle(getString(R.string.delete_location))
        builder.setMessage(getString(R.string.are_you_sure_delete_location))

        builder.setPositiveButton(getString(R.string.delete)) { _, _ ->
            onDeleteLocation()
        }
        builder.setNegativeButton(getString(R.string.cancel)) { _, _ -> }

        val dialog: AlertDialog = builder.create()
        dialog.show()
        dialog.getButton(AlertDialog.BUTTON_POSITIVE)
            .setTextSize(TypedValue.COMPLEX_UNIT_SP, 14.0f)
        dialog.getButton(AlertDialog.BUTTON_NEGATIVE)
            .setTextSize(TypedValue.COMPLEX_UNIT_SP, 14.0f)
    }

    private fun onDeleteLocation() {
        showLoading()
        deployment?.let {
            edgeDeploymentDb.deleteDeploymentLocation(it.id, object : DatabaseCallback {
                override fun onSuccess() {
                    DeploymentSyncWorker.enqueue(this@DeploymentDetailActivity)
                    hideLoading()
                    finish()
                }

                override fun onFailure(errorMessage: String) {
                    hideLoading()
                    showCommonDialog(errorMessage)
                }
            })
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == DEPLOYMENT_REQUEST_CODE) {
            forceUpdateDeployment()
        }
    }

    private fun forceUpdateDeployment() {
        if (this.deployment != null) {
            this.deployment = edgeDeploymentDb.getDeploymentById(this.deployment!!.id)
            this.deployment?.let { it1 -> updateDeploymentDetailView(it1) }
            supportActionBar?.apply {
                title = deployment?.location?.name ?: getString(R.string.title_deployment_detail)
            }
        }
    }

    private fun updateDeploymentDetailView(deployment: EdgeDeployment) {
        // setup deployment images view
        observeDeploymentImage(deployment.id)

        val location = deployment.location
        locationValueTextView.text =
            location?.let { locate ->
                convertLatLngLabel(this, locate.latitude, locate.longitude)
            }
    }

    private fun observeDeploymentImage(deploymentId: Int) {
        deployImageLiveData =
            Transformations.map(deploymentImageDb.getAllResultsAsync(deploymentId).asLiveData()) {
                it
            }
        deployImageLiveData.observeForever(deploymentImageObserve)
    }

    private fun updateDeploymentImages(deploymentImages: List<DeploymentImage>) {
        photoLabel.visibility = if (deploymentImages.isNotEmpty()) View.VISIBLE else View.GONE
        deploymentImageRecycler.visibility =
            if (deploymentImages.isNotEmpty()) View.VISIBLE else View.GONE
        val items = deploymentImages.map { it.toDeploymentImageView() }
        deploymentImageAdapter.submitList(items)
    }

    private fun setupImageRecycler() {
        deploymentImageRecycler.apply {
            adapter = deploymentImageAdapter
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
            setHasFixedSize(true)
        }
    }

    private fun setupToolbar() {
        setSupportActionBar(toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowHomeEnabled(true)
            title = deployment?.location?.name ?: getString(R.string.title_deployment_detail)
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }

    override fun onDestroy() {
        super.onDestroy()
        // remove observer
        deployImageLiveData.removeObserver(deploymentImageObserve)
    }

    companion object {
        const val DEPLOYMENT_REQUEST_CODE = 1001

        fun startActivity(context: Context, deploymentId: Int) {
            val intent = Intent(context, DeploymentDetailActivity::class.java)
            intent.putExtra(EXTRA_DEPLOYMENT_ID, deploymentId)
            context.startActivity(intent)
        }
    }
}
