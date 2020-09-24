package org.rfcx.audiomoth.view.deployment.guardian.connect

import android.graphics.Color
import android.net.wifi.ScanResult
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.item_guardian_hotspot.view.*
import org.rfcx.audiomoth.R

class GuardianHotspotAdapter(private val onHotspotClickListener: (ScanResult) -> Unit) : RecyclerView.Adapter<GuardianHotspotAdapter.GuardianHotspotViewHolder>() {

    var selectedPosition = -1

    var items: List<ScanResult> = arrayListOf()
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GuardianHotspotViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_guardian_hotspot, parent, false)
        return GuardianHotspotViewHolder(view)
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: GuardianHotspotViewHolder, position: Int) {
        if (selectedPosition == position) {
            holder.itemView.hotspotNameTextView.apply {
                setTextColor(ContextCompat.getColor(this.context, R.color.colorPrimary))
                setCompoundDrawablesWithIntrinsicBounds(
                    0,
                    0,
                    R.drawable.ic_hotspot_selected,
                    0
                )
            }
        } else {
            holder.itemView.hotspotNameTextView.apply {
                setTextColor(ContextCompat.getColor(this.context, R.color.text_secondary))
                setCompoundDrawablesWithIntrinsicBounds(
                    0,
                    0,
                    0,
                    0
                )
            }
        }
        val hotspot = items[position]
        holder.bind(hotspot)
        holder.itemView.setOnClickListener {
            selectedPosition = position
            notifyDataSetChanged()
            this.onHotspotClickListener(hotspot)
        }
    }

    inner class GuardianHotspotViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val hotspotName = itemView.hotspotNameTextView

        fun bind(hotspot: ScanResult) {
            hotspotName.text = hotspot.SSID
        }
    }
}
