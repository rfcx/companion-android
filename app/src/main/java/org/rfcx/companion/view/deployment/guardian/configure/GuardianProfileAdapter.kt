package org.rfcx.companion.view.deployment.guardian.configure

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.item_profile.view.*
import org.rfcx.companion.R
import org.rfcx.companion.entity.guardian.GuardianProfile
import org.rfcx.companion.entity.guardian.toReadableFormat

class GuardianProfilesAdapter(private val itemClickListener: (GuardianProfile) -> Unit) :
    RecyclerView.Adapter<GuardianProfilesAdapter.GuardianProfilesAdapterViewHolder>() {

    var items: List<GuardianProfile> = arrayListOf()
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GuardianProfilesAdapterViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_profile, parent, false)
        return GuardianProfilesAdapterViewHolder(view)
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: GuardianProfilesAdapterViewHolder, position: Int) {
        val profile = items[position]
        holder.bind(profile)
        holder.itemView.setOnClickListener {
            this.itemClickListener(profile)
        }
    }

    inner class GuardianProfilesAdapterViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val nameProfile = itemView.nameProfileTextView
        private val detailProfile = itemView.detailProfileTextView

        fun bind(profile: GuardianProfile) {
            val readableProfile = profile.asConfiguration().toReadableFormat()
            detailProfile.text = itemView.context.getString(R.string.configuration_details, readableProfile.fileFormat, readableProfile.sampleRate, readableProfile.bitrate, readableProfile.duration)
            nameProfile.text = profile.name
        }
    }
}
