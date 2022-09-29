package org.rfcx.companion.view.deployment.songmeter.detect

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.item_songmeter.view.*
import org.rfcx.companion.R
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
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_songmeter, parent, false)
        return SongMeterViewHolder(view)
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: SongMeterViewHolder, position: Int) {
        holder.itemView.setBackgroundColor(ContextCompat.getColor(holder.itemView.context, R.color.backgroundColor))

        val songMeter = items[position]
        holder.bind(songMeter)

        if (selectedPosition == position) {
            holder.itemView.apply {
                songMeterPrefixesTextView.setTextColor(ContextCompat.getColor(this.context, R.color.colorPrimary))
                songMeterSerialNumberTextView.setTextColor(ContextCompat.getColor(this.context, R.color.colorPrimary))
                songMeterPrefixesTextView.setCompoundDrawablesWithIntrinsicBounds(
                    0,
                    0,
                    R.drawable.ic_hotspot_selected,
                    0
                )
            }
        } else {
            holder.itemView.apply {
                songMeterPrefixesTextView.setTextColor(ContextCompat.getColor(this.context, R.color.text_secondary))
                songMeterSerialNumberTextView.setTextColor(ContextCompat.getColor(this.context, R.color.text_secondary))
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

    inner class SongMeterViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val prefixes = itemView.songMeterPrefixesTextView
        private val serialName = itemView.songMeterSerialNumberTextView

        fun bind(ads: Advertisement) {
            prefixes.text = ads.prefixes
            serialName.text = ads.serialName
        }
    }
}
