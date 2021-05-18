package org.rfcx.companion.view.project

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.item_location_group.view.*
import org.rfcx.companion.R
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

        holder.bind(items[position].name ?: holder.itemView.context.getString(R.string.none))

        holder.itemView.setOnClickListener {
            selectedPosition = position
            notifyDataSetChanged()
            this.projectSelectListener(items[position].id)
        }
    }

    override fun getItemCount(): Int = items.size

    inner class ProjectSelectViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val locationGroupTextView = itemView.locationGroupTextView

        fun bind(project: String) {
            locationGroupTextView.text = project
        }
    }
}
