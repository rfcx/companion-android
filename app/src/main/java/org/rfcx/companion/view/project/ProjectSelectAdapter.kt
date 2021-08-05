package org.rfcx.companion.view.project

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.item_location_group.view.*
import org.rfcx.companion.R
import org.rfcx.companion.entity.Permissions
import org.rfcx.companion.entity.Project
import org.rfcx.companion.entity.isGuest

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

    fun setClickable(view: View?, clickable: Boolean) {
        if (view != null) {
            if (view is ViewGroup) {
                val viewGroup = view
                for (i in 0 until viewGroup.childCount) {
                    setClickable(viewGroup.getChildAt(i), clickable)
                }
            }
            view.isClickable = clickable
        }
    }

    override fun getItemCount(): Int = items.size

    inner class ProjectSelectViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val locationGroupTextView = itemView.locationGroupTextView
        private val readOnlyLayout = itemView.readOnlyLayout

        fun bind(project: Project) {
            locationGroupTextView.text = project.name ?: itemView.context.getString(R.string.none)
            readOnlyLayout.visibility =
                if (project.isGuest()) View.VISIBLE else View.GONE
            setClickable(itemView, project.isGuest())

            if (project.isGuest()) {
                locationGroupTextView.setTextColor(ContextCompat.getColor(itemView.context, R.color.text_secondary))
                itemView.setBackgroundColor(ContextCompat.getColor(itemView.context, R.color.statusColor))
            } else {
                locationGroupTextView.setTextColor(ContextCompat.getColor(itemView.context, R.color.text_black))
                itemView.setBackgroundColor(ContextCompat.getColor(itemView.context, R.color.backgroundColor))
            }
        }
    }
}
