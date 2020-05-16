package org.rfcx.audiomoth.view.configure

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.item_profile.view.*
import org.rfcx.audiomoth.R
import org.rfcx.audiomoth.entity.Profile

class ProfilesAdapter : RecyclerView.Adapter<ProfilesAdapter.ProfilesAdapterViewHolder>() {

    var items: List<Profile> = arrayListOf()
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProfilesAdapterViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_profile, parent, false)
        return ProfilesAdapterViewHolder(view)
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: ProfilesAdapterViewHolder, position: Int) {
        holder.bind(items[position])
    }

    inner class ProfilesAdapterViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val nameProfile = itemView.nameProfileTextView
        private val detailProfile = itemView.detailProfileTextView

        fun bind(profile: Profile) {
            nameProfile.text = profile.name
            detailProfile.text = "${profile.sampleRate} kHz, ${profile.gain}"
        }
    }
}