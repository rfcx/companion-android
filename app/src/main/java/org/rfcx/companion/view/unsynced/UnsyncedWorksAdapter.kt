package org.rfcx.companion.view.unsynced

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
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
        notifyDataSetChanged()
    }

    companion object {
        const val DEPLOYMENT_ITEM = 1
        const val HEADER_ITEM = 3
    }

    override fun getItemViewType(position: Int): Int {
        return when (listOfUnsynced[position]) {
            is UnsyncedWorksViewItem.Deployment -> DEPLOYMENT_ITEM
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
            else -> HeaderItemViewHolder(
                LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_unsynced_header, parent, false)
            )
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder.itemViewType) {
            DEPLOYMENT_ITEM -> (holder as UnsyncedDeploymentViewHolder).bind(listOfUnsynced[position] as UnsyncedWorksViewItem.Deployment)
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

    inner class HeaderItemViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val headerName = itemView.findViewById<TextView>(R.id.unsyncedHeader)
        fun bind(header: UnsyncedWorksViewItem.Header) {
            headerName.text = header.name
        }
    }
}

interface UnsyncedWorkListener {
    fun onDeploymentClick(id: Int)
}
