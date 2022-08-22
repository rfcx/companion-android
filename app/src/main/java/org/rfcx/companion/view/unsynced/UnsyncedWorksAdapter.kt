package org.rfcx.companion.view.unsynced

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.item_register_guardian.view.*
import kotlinx.android.synthetic.main.item_unsynced_deployment.view.*
import kotlinx.android.synthetic.main.item_unsynced_deployment.view.deleteButton
import org.rfcx.companion.R
import org.rfcx.companion.adapter.UnsyncedWorksViewItem
import org.rfcx.companion.util.toTimeAgo

class UnsyncedWorksAdapter(private val unsyncedDeploymentListener: UnsyncedWorkListener) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private var listOfUnsynced = listOf<UnsyncedWorksViewItem>()

    fun setUnsynceds(unsynceds: List<UnsyncedWorksViewItem>) {
        listOfUnsynced = unsynceds
    }

    companion object {
        const val DEPLOYMENT_ITEM = 1
        const val REGISTRATION_ITEM = 2
        const val HEADER_ITEM = 3
    }

    override fun getItemViewType(position: Int): Int {
        return when (listOfUnsynced[position]) {
            is UnsyncedWorksViewItem.Deployment -> DEPLOYMENT_ITEM
            is UnsyncedWorksViewItem.Registration -> REGISTRATION_ITEM
            else -> HEADER_ITEM
        }
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): RecyclerView.ViewHolder {
        return when (viewType) {
            DEPLOYMENT_ITEM -> UnsyncedDeploymentViewHolder(
                LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_unsynced_deployment, parent, false)
            )
            REGISTRATION_ITEM -> UnsyncedRegistrationViewHolder(
                LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_register_guardian, parent, false)
            )
            else -> HeaderItemViewHolder(
                LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_checklist_header, parent, false)
            )
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder.itemViewType) {
            DEPLOYMENT_ITEM -> (holder as UnsyncedDeploymentViewHolder).bind(listOfUnsynced[position] as UnsyncedWorksViewItem.Deployment)
            REGISTRATION_ITEM ->(holder as UnsyncedRegistrationViewHolder).bind(listOfUnsynced[position] as UnsyncedWorksViewItem.Registration)
            else -> (holder as HeaderItemViewHolder).bind(listOfUnsynced[position] as UnsyncedWorksViewItem.Header)
        }
    }

    override fun getItemCount(): Int = listOfUnsynced.size

    inner class UnsyncedDeploymentViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val name = itemView.unsyncedName
        private val error = itemView.unsyncedError
        private val deployedAt = itemView.deployedAt
        val deleteButton = itemView.deleteButton

        fun bind(deployment: UnsyncedWorksViewItem.Deployment) {
            name.text = deployment.name
            if (deployment.error != null) {
                error.visibility = View.VISIBLE
                error.text = deployment.error
            } else {
                error.visibility = View.GONE
            }
            deployedAt.text = deployment.deployedAt.toTimeAgo(itemView.context)

            deleteButton.setOnClickListener {
                unsyncedDeploymentListener.onDeploymentClick(deployment.id)
            }
        }
    }

    inner class UnsyncedRegistrationViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val guid = itemView.registerGuardianName
        private val error = itemView.registerGuardianError
        val deleteButton = itemView.deleteButton

        fun bind(registration: UnsyncedWorksViewItem.Registration) {
            guid.text = registration.guid
            if (registration.error != null) {
                error.visibility = View.VISIBLE
                error.text = registration.error
            } else {
                error.visibility = View.GONE
            }
            deleteButton.setOnClickListener {
                unsyncedDeploymentListener.onRegistrationClick(registration.guid)
            }
        }
    }

    inner class HeaderItemViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val headerName = itemView.findViewById<TextView>(R.id.checkHeader)
        fun bind(header: UnsyncedWorksViewItem.Header) {
            headerName.text = header.name
        }
    }
}

interface UnsyncedWorkListener {
    fun onDeploymentClick(id: Int)
    fun onRegistrationClick(id: String)
}
