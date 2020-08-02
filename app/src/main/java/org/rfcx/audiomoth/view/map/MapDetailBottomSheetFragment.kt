package org.rfcx.audiomoth.view.map

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import io.realm.Realm
import kotlinx.android.synthetic.main.fragment_map_detail_bottom_sheet.*
import org.rfcx.audiomoth.MainActivityListener
import org.rfcx.audiomoth.R
import org.rfcx.audiomoth.entity.DeploymentState
import org.rfcx.audiomoth.entity.Device
import org.rfcx.audiomoth.entity.EdgeDeployment
import org.rfcx.audiomoth.entity.guardian.GuardianDeployment
import org.rfcx.audiomoth.localdb.EdgeDeploymentDb
import org.rfcx.audiomoth.localdb.guardian.GuardianDeploymentDb
import org.rfcx.audiomoth.util.Battery.getEstimatedBatteryDuration
import org.rfcx.audiomoth.util.RealmHelper
import org.rfcx.audiomoth.util.WifiHotspotUtils
import org.rfcx.audiomoth.util.toDateString
import org.rfcx.audiomoth.view.deployment.DeploymentActivity
import org.rfcx.audiomoth.view.detail.DeploymentDetailActivity
import org.rfcx.audiomoth.view.diagnostic.DiagnosticActivity
import java.util.*

class MapDetailBottomSheetFragment : Fragment() {
    private val edgeDeploymentDb =
        EdgeDeploymentDb(Realm.getInstance(RealmHelper.migrationConfig()))
    private val guardianDeploymentDb =
        GuardianDeploymentDb(Realm.getInstance(RealmHelper.migrationConfig()))
    private var id: Int? = null
    private var device: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initIntent()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_map_detail_bottom_sheet, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val id: Int = this.id ?: -1

        if (device == Device.EDGE.value) {
            val deployment = edgeDeploymentDb.getDeploymentById(id)
            bindEdgeDeploymentView(deployment)
        } else {
            val guardianDeployment = guardianDeploymentDb.getDeploymentById(id)
            bindGuardianDeploymentView(guardianDeployment)
        }
    }

    private fun initIntent() {
        arguments?.let {
            id = it.getInt(ARG_DEPLOYMENT_ID)
            device = it.getString(ARG_DEVICE)
        }
    }

    private fun bindEdgeDeploymentView(deployment: EdgeDeployment?) {
        if (deployment != null) {
            val isStateReadyToUpload = deployment.state == DeploymentState.Edge.ReadyToUpload.key
            locationNameTextView.text = deployment.location?.name

            dateTextView.text = if (isStateReadyToUpload) getString(
                R.string.deploy_at,
                Date(deployment.deployedAt.time).toDateString()
            ) else getString(R.string.no_deployment)

            seeDetailTextView.text =
                if (isStateReadyToUpload) getString(R.string.see_deployment_detail) else getString(
                    R.string.create_deployment
                )

            mapDetailBottomSheetView.setOnClickListener {
                if (isStateReadyToUpload) {
                    context?.let { context ->
                        DeploymentDetailActivity.startActivity(context, deployment.id)
                    }
                    (activity as MainActivityListener).hideBottomSheet()
                } else {
                    context?.let {
                        DeploymentActivity.startActivity(it, deployment.id)
                    }
                }
            }

            estimatedBatteryDurationTextView.visibility =
                if (isStateReadyToUpload) View.VISIBLE else View.GONE
            estimatedBatteryDurationTextView.text =
                context?.let { getEstimatedBatteryDuration(it, deployment.batteryDepletedAt.time) }
            guardianTextView.visibility = View.GONE
        }
    }

    private fun bindGuardianDeploymentView(guardianDeployment: GuardianDeployment?) {
        if (guardianDeployment != null) {
            locationNameTextView.text = guardianDeployment.location?.name
            seeDetailTextView.text = getString(R.string.see_deployment_detail)
            estimatedBatteryDurationTextView.visibility = View.GONE
            guardianTextView.visibility = View.VISIBLE

            dateTextView.text = getString(
                R.string.deploy_at,
                Date(guardianDeployment.deployedAt.time).toDateString()
            )

            mapDetailBottomSheetView.setOnClickListener {
                context?.let {
                    DiagnosticActivity.startActivity(
                        it,
                        guardianDeployment,
                        WifiHotspotUtils.isConnectedWithGuardian(
                            requireContext(),
                            guardianDeployment.wifiName ?: ""
                        )
                    )
                }
            }
        }
    }

    companion object {
        private const val ARG_DEPLOYMENT_ID = "ARG_DEPLOYMENT_ID"
        private const val ARG_DEVICE = "ARG_DEVICE"

        @JvmStatic
        fun newInstance(id: Int, device: String) =
            MapDetailBottomSheetFragment().apply {
                arguments = Bundle().apply {
                    putInt(ARG_DEPLOYMENT_ID, id)
                    putString(ARG_DEVICE, device)
                }
            }
    }
}
