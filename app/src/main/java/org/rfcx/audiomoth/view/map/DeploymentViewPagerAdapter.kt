package org.rfcx.audiomoth.view.map

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.item_deployment_detail.view.*
import org.rfcx.audiomoth.R
import org.rfcx.audiomoth.entity.DeploymentState
import org.rfcx.audiomoth.util.Battery
import org.rfcx.audiomoth.util.getIntColor
import org.rfcx.audiomoth.util.toDateString
import java.util.*

interface DeploymentDetailClickListener {
    fun onClickedMoreIcon(edgeDeploymentView: DeploymentDetailView.EdgeDeploymentView)
    fun onClickedEdgeDeploymentDetail(edgeDeploymentView: DeploymentDetailView.EdgeDeploymentView)
    fun onClickedGuardianDeploymentDetail(guardianDeploymentView: DeploymentDetailView.GuardianDeploymentView)
}

class DeploymentViewPagerAdapter(private val itemClickListener: DeploymentDetailClickListener) :
    ListAdapter<DeploymentDetailView, RecyclerView.ViewHolder>(DeploymentDetailViewDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            DEPLOYMENT_EDGE_VIEW -> {
                EdgeDeploymentDetailViewHolder(
                    layoutInflater.inflate(R.layout.item_deployment_detail, parent, false),
                    itemClickListener
                )
            }
            DEPLOYMENT_GUARDIAN_VIEW -> {
                GuardianDeploymentDetailViewHolder(
                    layoutInflater.inflate(R.layout.item_deployment_detail, parent, false),
                    itemClickListener
                )
            }
            else -> throw IllegalStateException("View type '$viewType' miss match on DeploymentViewPagerAdapter")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val item = getItem(position)
        when {
            holder is EdgeDeploymentDetailViewHolder
                    && item is DeploymentDetailView.EdgeDeploymentView -> {
                holder.bind(item)
            }
            holder is GuardianDeploymentDetailViewHolder
                    && item is DeploymentDetailView.GuardianDeploymentView -> {
                holder.bind(item)
            }
        }
    }

    override fun getItemViewType(position: Int): Int {
        return getItem(position).viewType
    }

    fun getItemByPosition(position: Int): DeploymentDetailView? {
        return getItem(position)
    }

    companion object {
        const val DEPLOYMENT_EDGE_VIEW = 1
        const val DEPLOYMENT_GUARDIAN_VIEW = 2
    }
}

class EdgeDeploymentDetailViewHolder(
    itemView: View,
    private val itemClickListener: DeploymentDetailClickListener
) : RecyclerView.ViewHolder(itemView) {
    private val context = itemView.context
    private val tvLocation = itemView.locationNameTextView
    private val tvDate = itemView.dateTextView
    private val tvSeeDetail = itemView.seeDetailTextView
    private val tvGuardianBadge = itemView.guardianBadgeTextView
    private val tvEstimatedBatteryDuration = itemView.estimatedBatteryDurationTextView
    private val ivMoreIcon = itemView.moreIconImageView
    private val vDeploymentDetail = itemView.deploymentDetailView
    private val ivSync = itemView.syncImageView
    private val batteryComponent = itemView.batteryComponent
    private val vMoreIconView = itemView.moreIconView

    fun bind(deployment: DeploymentDetailView.EdgeDeploymentView) {
        val isReadyToUpload = deployment.state == DeploymentState.Edge.ReadyToUpload.key

        ivSync.setImageDrawable(
            ContextCompat.getDrawable(
                itemView.context,
                deployment.syncImage
            )
        )
        val estimatedBatteryDays =
            Battery.getEstimatedBatteryDays(deployment.batteryDepletedAt.time)
        batteryComponent.levelBattery = if(estimatedBatteryDays >= 0) estimatedBatteryDays else -1

        tvGuardianBadge.visibility = View.GONE
        tvLocation.text = deployment.locationName
        tvDate.text = if (isReadyToUpload) {
            context.getString(R.string.deploy_at, Date(deployment.deployedAt.time).toDateString())
        } else {
            context.getString(R.string.no_deployment)
        }
        tvSeeDetail.text = context.getString(
            if (isReadyToUpload) R.string.see_deployment_detail else R.string.create_deployment
        )
        batteryComponent.visibility = if (isReadyToUpload) View.VISIBLE else View.GONE
        ivMoreIcon.visibility = if (isReadyToUpload) View.GONE else View.VISIBLE
        tvEstimatedBatteryDuration.visibility = if (isReadyToUpload) View.VISIBLE else View.GONE
        tvEstimatedBatteryDuration.text =
            Battery.getEstimatedBatteryDuration(context, deployment.batteryDepletedAt.time)
        vDeploymentDetail.setOnClickListener {
            itemClickListener.onClickedEdgeDeploymentDetail(deployment)
        }
        vMoreIconView.setOnClickListener {
            itemClickListener.onClickedMoreIcon(deployment)
        }
    }
}

class GuardianDeploymentDetailViewHolder(
    itemView: View,
    private val itemClickListener: DeploymentDetailClickListener
) : RecyclerView.ViewHolder(itemView) {
    private val context = itemView.context
    private val tvLocation = itemView.locationNameTextView
    private val tvDate = itemView.dateTextView
    private val tvSeeDetail = itemView.seeDetailTextView
    private val tvGuardianBadge = itemView.guardianBadgeTextView
    private val tvEstimatedBatteryDuration = itemView.estimatedBatteryDurationTextView
    private val ivMoreIcon = itemView.moreIconImageView
    private val vDeploymentDetail = itemView.deploymentDetailView
    private val vMoreIconView = itemView.moreIconView
    private val ivSync = itemView.syncImageView
    private val batteryComponent = itemView.batteryComponent
    private val itemLayout = itemView.deploymentDetailLayout

    fun bind(deployment: DeploymentDetailView.GuardianDeploymentView) {
        tvGuardianBadge.visibility = View.VISIBLE
        tvEstimatedBatteryDuration.visibility = View.GONE
        ivMoreIcon.visibility = View.GONE
        vDeploymentDetail.visibility = View.GONE
        vMoreIconView.visibility = View.GONE
        batteryComponent.visibility = View.GONE
        tvLocation.text = deployment.locationName
        tvSeeDetail.text = context.getString(R.string.see_deployment_detail)
        tvDate.text =
            context.getString(R.string.deploy_at, Date(deployment.deployedAt.time).toDateString())

        itemLayout.setOnClickListener {
            itemClickListener.onClickedGuardianDeploymentDetail(deployment)
        }
        ivSync.setImageDrawable(
            ContextCompat.getDrawable(
                itemView.context,
                deployment.syncImage
            )
        )
    }
}

class DeploymentDetailViewDiffCallback : DiffUtil.ItemCallback<DeploymentDetailView>() {
    override fun areItemsTheSame(
        oldItem: DeploymentDetailView,
        newItem: DeploymentDetailView
    ): Boolean {
        return when {
            oldItem is DeploymentDetailView.EdgeDeploymentView
                    && newItem is DeploymentDetailView.EdgeDeploymentView -> {
                oldItem.id == newItem.id
            }
            oldItem is DeploymentDetailView.GuardianDeploymentView
                    && newItem is DeploymentDetailView.GuardianDeploymentView -> {
                oldItem.id == newItem.id
            }
            else -> false
        }
    }

    override fun areContentsTheSame(
        oldItem: DeploymentDetailView,
        newItem: DeploymentDetailView
    ): Boolean {
        return when {
            oldItem is DeploymentDetailView.EdgeDeploymentView
                    && newItem is DeploymentDetailView.EdgeDeploymentView -> {
                oldItem.batteryDepletedAt == newItem.batteryDepletedAt
                        && oldItem.batteryLevel == newItem.batteryLevel
                        && oldItem.createdAt == newItem.createdAt
                        && oldItem.deletedAt == newItem.deletedAt
                        && oldItem.deployedAt == newItem.deployedAt
                        && oldItem.state == newItem.state
                        && oldItem.syncState == newItem.syncState
                        && oldItem.deploymentId == newItem.deploymentId
                        && oldItem.locationName == newItem.locationName
                        && oldItem.longitude == newItem.longitude
                        && oldItem.latitude == newItem.latitude
            }
            oldItem is DeploymentDetailView.GuardianDeploymentView
                    && newItem is DeploymentDetailView.GuardianDeploymentView -> {
                oldItem.createdAt == newItem.createdAt
                        && oldItem.deployedAt == newItem.deployedAt
                        && oldItem.state == newItem.state
                        && oldItem.syncState == newItem.syncState
                        && oldItem.wifiName == newItem.wifiName
                        && oldItem.locationName == newItem.locationName
                        && oldItem.longitude == newItem.longitude
                        && oldItem.latitude == newItem.latitude
            }
            else -> false
        }
    }
}
