package org.rfcx.companion.view.project

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import org.rfcx.companion.R
import org.rfcx.companion.databinding.ItemLocationGroupBinding
import org.rfcx.companion.entity.Permissions
import org.rfcx.companion.entity.Project
import org.rfcx.companion.entity.isGuest
import org.rfcx.companion.view.profile.locationgroup.ProjectListener

class ProjectSelectAdapter(private val projectSelectListener: ProjectListener) :
    RecyclerView.Adapter<ProjectSelectAdapter.ProjectSelectViewHolder>() {

    var selectedPosition = -1
    var items: List<Project> = arrayListOf()
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProjectSelectViewHolder {
        val binding =
            ItemLocationGroupBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ProjectSelectViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ProjectSelectViewHolder, position: Int) {

        if (selectedPosition == position) {
            holder.binding.checkImageView.visibility = View.VISIBLE
        } else {
            holder.binding.checkImageView.visibility = View.GONE
        }

        holder.bind(items[position])

        holder.itemView.setOnClickListener {
            if (items[position].permissions != Permissions.GUEST.value) {
                selectedPosition = position
                notifyDataSetChanged()
                projectSelectListener.onClicked(items[position])
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

    inner class ProjectSelectViewHolder(val binding: ItemLocationGroupBinding) : RecyclerView.ViewHolder(binding.root) {
        private val locationGroupTextView = binding.locationGroupTextView
        private val lockImageView = binding.lockImageView

        fun bind(project: Project) {
            locationGroupTextView.text = project.name ?: itemView.context.getString(R.string.none)
            lockImageView.visibility =
                if (project.isGuest()) View.VISIBLE else View.GONE
            setClickable(itemView, project.isGuest())

            if (project.isGuest()) {
                locationGroupTextView.setTextColor(ContextCompat.getColor(itemView.context, R.color.text_secondary))
            } else {
                locationGroupTextView.setTextColor(ContextCompat.getColor(itemView.context, R.color.text_black))
            }

            lockImageView.setColorFilter(
                ContextCompat.getColor(
                    itemView.context,
                    R.color.text_secondary
                )
            )

            lockImageView.setOnClickListener {
                projectSelectListener.onLockImageClicked()
            }
        }
    }
}
