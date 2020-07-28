package org.rfcx.audiomoth.view.map

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import io.realm.Realm
import java.util.*
import kotlin.math.roundToInt
import kotlinx.android.synthetic.main.fragment_map_detail_bottom_sheet.*
import org.rfcx.audiomoth.R
import org.rfcx.audiomoth.entity.Deployment
import org.rfcx.audiomoth.entity.DeploymentState
import org.rfcx.audiomoth.entity.Device
import org.rfcx.audiomoth.entity.guardian.GuardianDeployment
import org.rfcx.audiomoth.localdb.DeploymentDb
import org.rfcx.audiomoth.localdb.guardian.GuardianDeploymentDb
import org.rfcx.audiomoth.util.RealmHelper
import org.rfcx.audiomoth.util.WifiHotspotUtils
import org.rfcx.audiomoth.util.toDateString
import org.rfcx.audiomoth.view.deployment.DeploymentActivity
import org.rfcx.audiomoth.view.detail.DeploymentDetailActivity
import org.rfcx.audiomoth.view.diagnostic.DiagnosticActivity

class MapDetailBottomSheetFragment : Fragment() {
    private val deploymentDb = DeploymentDb(Realm.getInstance(RealmHelper.migrationConfig()))
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
            val deployment = deploymentDb.getDeploymentById(id)
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

    private fun bindEdgeDeploymentView(deployment: Deployment?) {
        if (deployment != null) {
            val isStateReadyToUpload = deployment.state == DeploymentState.Edge.ReadyToUpload.key
            locationNameTextView.text = deployment.location?.name

            dateTextView.text = if (isStateReadyToUpload) getString(
                R.string.deploy_at,
                Date(deployment.deployedAt.time).toDateString()
            ) else getString(R.string.create_at, Date(deployment.createdAt.time).toDateString())

            seeDetailTextView.text =
                if (isStateReadyToUpload) getString(R.string.see_deployment_detail) else getString(
                    R.string.create_deployment
                )

            mapDetailBottomSheetView.setOnClickListener {
                if (isStateReadyToUpload) {
                    context?.let { context ->
                        DeploymentDetailActivity.startActivity(context, deployment.id)
                    }
                } else {
                    context?.let {
                        DeploymentActivity.startActivity(it, deployment.id)
                    }
                }
            }

            estimatedBatteryDurationTextView.visibility =
                if (isStateReadyToUpload) View.VISIBLE else View.GONE
            estimatedBatteryDurationTextView.text =
                getEstimatedBatteryDuration(deployment.batteryDepletedAt.time)
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

    private fun getEstimatedBatteryDuration(timestamp: Long): String {
        val currentMillis = System.currentTimeMillis()
        return if (timestamp > currentMillis) {
            val numberOfDate = ((timestamp - currentMillis) / (DAY).toDouble()).roundToInt()
            if (numberOfDate > 1) getString(
                R.string.days_remaining,
                numberOfDate
            ) else getString(R.string.day_remaining, numberOfDate)
        } else {
            getString(R.string.battery_depleted)
        }
    }

    companion object {
        private const val ARG_DEPLOYMENT_ID = "ARG_DEPLOYMENT_ID"
        private const val ARG_DEVICE = "ARG_DEVICE"
        private const val DAY = 24 * 60 * 60 * 1000

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
