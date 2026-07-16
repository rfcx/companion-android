package org.rfcx.companion.view.deployment.songmeter.detect

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import org.rfcx.companion.R
import org.rfcx.companion.databinding.ItemSongmeterBinding
import org.rfcx.companion.entity.songmeter.Advertisement

class SongMeterAdapter(private val onRecorderClickListener: (Advertisement) -> Unit) : RecyclerView.Adapter<SongMeterAdapter.SongMeterViewHolder>() {

    var selectedPosition = -1

    var items: List<Advertisement> = arrayListOf()
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    fun clear() {
        selectedPosition = -1
        items = listOf()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SongMeterViewHolder {
        val binding = ItemSongmeterBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return SongMeterViewHolder(binding)
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: SongMeterViewHolder, position: Int) {
        holder.itemView.setBackgroundColor(ContextCompat.getColor(holder.itemView.context, R.color.backgroundColor))

        val songMeter = items[position]
        holder.bind(songMeter)

        if (selectedPosition == position) {
            holder.binding.apply {
                songMeterPrefixesTextView.setTextColor(ContextCompat.getColor(root.context, R.color.colorPrimary))
                songMeterSerialNumberTextView.setTextColor(ContextCompat.getColor(root.context, R.color.colorPrimary))
                songMeterPrefixesTextView.setCompoundDrawablesWithIntrinsicBounds(
                    0,
                    0,
                    R.drawable.ic_hotspot_selected,
                    0
                )
            }
        } else {
            holder.binding.apply {
                songMeterPrefixesTextView.setTextColor(ContextCompat.getColor(root.context, R.color.text_secondary))
                songMeterSerialNumberTextView.setTextColor(ContextCompat.getColor(root.context, R.color.text_secondary))
                songMeterPrefixesTextView.setCompoundDrawablesWithIntrinsicBounds(
                    0,
                    0,
                    0,
                    0
                )
            }
        }

        holder.itemView.setOnClickListener {
            selectedPosition = position
            notifyDataSetChanged()
            this.onRecorderClickListener(songMeter)
        }
    }

    inner class SongMeterViewHolder(val binding: ItemSongmeterBinding) : RecyclerView.ViewHolder(binding.root) {
        private val prefixes = binding.songMeterPrefixesTextView
        private val serialName = binding.songMeterSerialNumberTextView

        fun bind(ads: Advertisement) {
            prefixes.text = ads.prefixes
            serialName.text = ads.serialName
        }
    }
}
