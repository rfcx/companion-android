package org.rfcx.companion.view.deployment.guardian.softwareupdate

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.item_text_and_checkmark.view.*
import org.rfcx.companion.R

class GuardianApkAdapter(private val onClickListener: (String) -> Unit) :
    RecyclerView.Adapter<GuardianApkAdapter.GuardianApkViewHolder>() {
    var selectedPosition = -1
    var items: List<String> = arrayListOf()
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): GuardianApkAdapter.GuardianApkViewHolder {
        val view =
            LayoutInflater.from(parent.context).inflate(R.layout.item_text_and_checkmark, parent, false)
        return GuardianApkViewHolder(view)
    }

    override fun onBindViewHolder(holder: GuardianApkAdapter.GuardianApkViewHolder, position: Int) {
        if (selectedPosition == position) {
            holder.itemView.checkImageView.visibility = View.VISIBLE
        } else {
            holder.itemView.checkImageView.visibility = View.GONE
        }

        holder.bind(items[position])
        holder.itemView.setOnClickListener {
            selectedPosition = position
            notifyDataSetChanged()
            onClickListener(items[position])

        }
    }

    override fun getItemCount(): Int = items.size

    inner class GuardianApkViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val versionApkTextView = itemView.nameTextView
        fun bind(versionApk: String) {
            versionApkTextView.text = versionApk
        }
    }
}
