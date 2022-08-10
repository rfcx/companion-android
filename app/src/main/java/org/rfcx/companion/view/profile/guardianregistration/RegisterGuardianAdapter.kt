package org.rfcx.companion.view.profile.guardianregistration

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.item_register_guardian.view.*
import org.rfcx.companion.R
import org.rfcx.companion.entity.RegisterGuardian

class RegisterGuardianAdapter(private val registerGuardianListener: RegisterGuardianListener) :
    RecyclerView.Adapter<RegisterGuardianAdapter.RegisterGuardianViewHolder>() {

    var items: List<RegisterGuardian> = listOf()
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RegisterGuardianViewHolder {
        val view =
            LayoutInflater.from(parent.context).inflate(R.layout.item_register_guardian, parent, false)
        return RegisterGuardianViewHolder(view)
    }

    override fun onBindViewHolder(holder: RegisterGuardianViewHolder, position: Int) {
        holder.bind(items[position])
        holder.deleteButton.setOnClickListener {
            registerGuardianListener.onClick(items[position].guid)
        }
    }

    override fun getItemCount(): Int = items.size

    inner class RegisterGuardianViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val guid = itemView.registerGuardianName
        private val error = itemView.registerGuardianError
        val deleteButton = itemView.deleteButton

        fun bind(registration: RegisterGuardian) {
            guid.text = registration.guid
            if (registration.error != null) {
                error.visibility = View.VISIBLE
                error.text = registration.error
            } else {
                error.visibility = View.GONE
            }
        }
    }
}

interface RegisterGuardianListener {
    fun onClick(guid: String)
}
