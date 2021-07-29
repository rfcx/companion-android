package org.rfcx.companion.view.project

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.item_location_group.view.*
import org.rfcx.companion.R
import org.rfcx.companion.entity.Permissions
import org.rfcx.companion.entity.Project

class ProjectSelectAdapter(private val projectSelectListener: (Int) -> Unit) :
    RecyclerView.Adapter<ProjectSelectAdapter.ProjectSelectViewHolder>() {

    var selectedPosition = -1
    var items: List<Project> = arrayListOf()
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProjectSelectViewHolder {
        val view =
            LayoutInflater.from(parent.context).inflate(R.layout.item_location_group, parent, false)
        return ProjectSelectViewHolder(view)
    }

    override fun onBindViewHolder(holder: ProjectSelectViewHolder, position: Int) {

        if (selectedPosition == position) {
            holder.itemView.checkImageView.visibility = View.VISIBLE
        } else {
            holder.itemView.checkImageView.visibility = View.GONE
        }

        holder.bind(items[position])

        holder.itemView.setOnClickListener {
            if (items[position].permissions != Permissions.GUEST.value) {
                selectedPosition = position
                notifyDataSetChanged()
                this.projectSelectListener(items[position].id)
            }
        }
    }

    override fun getItemCount(): Int = items.size

    inner class ProjectSelectViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val locationGroupTextView = itemView.locationGroupTextView
        private val permissionsTextView = itemView.permissionsTextView
        private val lockImageView = itemView.lockImageView

        fun bind(project: Project) {
            locationGroupTextView.text = project.name ?: itemView.context.getString(R.string.none)
            permissionsTextView.text = project.permissions
            lockImageView.visibility =
                if (project.permissions == Permissions.GUEST.value) View.VISIBLE else View.GONE
        }
    }
}
