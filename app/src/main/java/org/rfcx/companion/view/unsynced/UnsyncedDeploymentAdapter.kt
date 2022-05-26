package org.rfcx.companion.view.unsynced

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.item_unsynced_deployment.view.*
import org.rfcx.companion.R
import org.rfcx.companion.entity.UnsyncedDeployment

class UnsyncedDeploymentAdapter(private val unsyncedDeploymentListener: UnsyncedDeploymentListener) :
    RecyclerView.Adapter<UnsyncedDeploymentAdapter.UnsyncedDeploymentViewHolder>() {

    var items: List<UnsyncedDeployment> = listOf()
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UnsyncedDeploymentViewHolder {
        val view =
            LayoutInflater.from(parent.context).inflate(R.layout.item_unsynced_deployment, parent, false)
        return UnsyncedDeploymentViewHolder(view)
    }

    override fun onBindViewHolder(holder: UnsyncedDeploymentViewHolder, position: Int) {
        holder.bind(items[position])
        holder.deleteButton.setOnClickListener {
            unsyncedDeploymentListener.onClick(items[position].id)
        }
    }

    override fun getItemCount(): Int = items.size

    inner class UnsyncedDeploymentViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val name = itemView.unsyncedName
        private val error = itemView.unsyncedError
        val deleteButton = itemView.deleteButton

        fun bind(unsynced: UnsyncedDeployment) {
            name.text = unsynced.name
            error.text = unsynced.error
        }
    }
}

interface UnsyncedDeploymentListener {
    fun onClick(id: Int)
}
