package org.rfcx.companion.view.deployment.songmeter.detect

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.item_songmeter.view.*
import org.rfcx.companion.R
import org.rfcx.companion.entity.songmeter.Advertisement

class SongMeterAdapter(private val onRecorderClickListener: (Advertisement) -> Unit) : RecyclerView.Adapter<SongMeterAdapter.SongMeterViewHolder>() {

    var items: List<Advertisement> = arrayListOf()
        set(value) {
            field = value
            notifyDataSetChanged()
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
        holder.pairButton.setOnClickListener {
            this.onRecorderClickListener(songMeter)
        }
    }

    inner class SongMeterViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val prefixes = itemView.songMeterPrefixesTextView
        private val serialName = itemView.songMeterSerialNumberTextView
        val pairButton: AppCompatTextView = itemView.songMeterPair

        fun bind(ads: Advertisement) {
            prefixes.text = ads.prefixes
            serialName.text = ads.serialName

            if (ads.isReadyToPair) {
                pairButton.visibility = View.VISIBLE
            } else {
                pairButton.visibility = View.GONE
            }
        }
    }
}
