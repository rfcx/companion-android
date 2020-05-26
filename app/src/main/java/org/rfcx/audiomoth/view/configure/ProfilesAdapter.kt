package org.rfcx.audiomoth.view.configure

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.item_profile.view.*
import org.rfcx.audiomoth.R
import org.rfcx.audiomoth.entity.Profile

class ProfilesAdapter(private val itemClickListener: (Profile) -> Unit) :
    RecyclerView.Adapter<ProfilesAdapter.ProfilesAdapterViewHolder>() {

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
        val profile = items[position]
        holder.bind(profile)
        holder.itemView.setOnClickListener {
            this.itemClickListener(profile)
        }
    }

    inner class ProfilesAdapterViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val nameProfile = itemView.nameProfileTextView
        private val detailProfile = itemView.detailProfileTextView

        fun bind(profile: Profile) {
            val gainLabel = itemView.resources.getStringArray(R.array.gainLabel)[profile.gain - 1]
            var time = ""
            if (profile.recordingPeriodList.size == 1) {
                time = "(${profile.recordingPeriodList[0]})"
            } else {
                profile.recordingPeriodList.map {
                    time += when (it) {
                        profile.recordingPeriodList[0] -> "($it, "
                        profile.recordingPeriodList.last() -> "$it)"
                        else -> "$it, "
                    }
                }
            }

            val duration = if (profile.durationSelected == "CONTINUOUS") {
                "${itemView.context.getString(R.string.continuous).decapitalize()}"
            } else {
                "${profile.recordingDuration} sec/ ${profile.sleepDuration} sec"
            }

            var detail = "${profile.sampleRate} kHz, ${gainLabel}, $duration"

            if (profile.recordingPeriodList.isNotEmpty()) {
                detail = "${profile.sampleRate} kHz, ${gainLabel}, $duration, $time"
            }

            detailProfile.text = detail
            nameProfile.text = profile.name
        }
    }
}