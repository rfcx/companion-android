package org.rfcx.companion.view.deployment.guardian.classifierloader

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.item_location_group.view.*
import org.rfcx.companion.R
import org.rfcx.companion.entity.Project

class ClassifiierAdapter() :
    RecyclerView.Adapter<ClassifiierAdapter.ClassifiierViewHolder>() {

    var selectedPosition = -1
    var items: List<Project> = arrayListOf()
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ClassifiierViewHolder {
        val view =
            LayoutInflater.from(parent.context).inflate(R.layout.expandable_child_item, parent, false)
        return ClassifiierViewHolder(view)
    }

    override fun onBindViewHolder(holder: ClassifiierViewHolder, position: Int) {
        holder.bind(items[position])

        holder.itemView.setOnClickListener {

        }
    }

    override fun getItemCount(): Int = items.size

    inner class ClassifiierViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val locationGroupTextView = itemView.locationGroupTextView
        private val lockImageView = itemView.lockImageView

        fun bind(project: Project) {

        }
    }
}
