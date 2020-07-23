package org.rfcx.audiomoth.view.map

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import io.realm.Realm
import kotlinx.android.synthetic.main.fragment_map_detail_bottom_sheet.*
import org.rfcx.audiomoth.R
import org.rfcx.audiomoth.entity.Deployment
import org.rfcx.audiomoth.entity.DeploymentState
import org.rfcx.audiomoth.localdb.DeploymentDb
import org.rfcx.audiomoth.util.RealmHelper
import org.rfcx.audiomoth.util.toDateTimeString
import org.rfcx.audiomoth.view.deployment.DeploymentActivity
import org.rfcx.audiomoth.view.detail.DetailDeploymentActivity
import java.util.*

class MapDetailBottomSheetFragment : Fragment() {
    private val deploymentDb = DeploymentDb(Realm.getInstance(RealmHelper.migrationConfig()))
    private var id: Int? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initIntent()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_map_detail_bottom_sheet, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val id: Int = this.id ?: -1
        val deployment = deploymentDb.getDeploymentById(id)

        bindDeploymentView(deployment)
    }

    private fun initIntent() {
        arguments?.let {
            id = it.getInt(ARG_ID)
        }
    }

    private fun bindDeploymentView(deployment: Deployment?) {
        if (deployment != null) {
            locationNameTextView.text = deployment.location?.name
            seeDetailTextView.text =
                if (deployment.state == DeploymentState.Edge.ReadyToUpload.key) getString(R.string.see_deployment_detail) else getString(
                    R.string.create_deployment
                )

            dateTextView.text =
                if (deployment.state == DeploymentState.Edge.ReadyToUpload.key) getString(
                    R.string.deploy_at,
                    Date(deployment.deployedAt.time).toDateTimeString()
                ) else getString(
                    R.string.create_at,
                    Date(deployment.createdAt.time).toDateTimeString()
                )

            seeDetailTextView.setOnClickListener {
                if (deployment.state == DeploymentState.Edge.ReadyToUpload.key) {
                    context?.let { context ->
                        id?.let { id ->
                            DetailDeploymentActivity.startActivity(context, id)
                        }
                    }
                } else {
                    context?.let {
                        DeploymentActivity.startActivity(it, deployment.id)
                    }
                }
            }
        }
    }

    companion object {
        private const val ARG_ID = "ARG_ID"

        @JvmStatic
        fun newInstance(id: Int) =
            MapDetailBottomSheetFragment().apply {
                arguments = Bundle().apply {
                    putInt(ARG_ID, id)
                }
            }
    }
}
