package org.rfcx.companion.view.profile.locationgroup

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.item_location_group.view.*
import org.rfcx.companion.R
import org.rfcx.companion.entity.Project
import org.rfcx.companion.entity.Screen
import org.rfcx.companion.entity.isGuest

class ProjectAdapter(private val projectListener: ProjectListener) :
    RecyclerView.Adapter<ProjectAdapter.ProjectViewHolder>() {
    var selectedGroup: String = ""
    var screen: String = ""
    var items: List<Project> = arrayListOf()
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ProjectViewHolder {
        val view =
            LayoutInflater.from(parent.context).inflate(R.layout.item_location_group, parent, false)
        return ProjectViewHolder(view)
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: ProjectViewHolder, position: Int) {
        holder.bind(items[position])
        holder.itemView.setOnClickListener {
            projectListener.onClicked(items[position])
        }
    }

    inner class ProjectViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val projectTextView = itemView.locationGroupTextView
        private val checkImageView = itemView.checkImageView
        private val lockImageView = itemView.lockImageView

        fun bind(project: Project) {
            if (screen != Screen.PROFILE.id) {
                checkImageView.visibility =
                    if (project.name == selectedGroup) View.VISIBLE else View.GONE
            }

            projectTextView.text = project.name ?: itemView.context.getString(R.string.none)

            lockImageView.visibility =
                if (project.isGuest()) View.VISIBLE else View.GONE
            setClickable(itemView, project.isGuest())

            if (project.isGuest()) {
                projectTextView.setTextColor(ContextCompat.getColor(itemView.context, R.color.text_secondary))
            } else {
                projectTextView.setTextColor(ContextCompat.getColor(itemView.context, R.color.text_black))
            }

            lockImageView.setColorFilter(
                ContextCompat.getColor(
                    itemView.context,
                    R.color.text_secondary
                )
            )

            lockImageView.setOnClickListener {
                projectListener.onLockImageClicked()
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
}

interface ProjectListener {
    fun onClicked(project: Project)
    fun onLockImageClicked()
}
