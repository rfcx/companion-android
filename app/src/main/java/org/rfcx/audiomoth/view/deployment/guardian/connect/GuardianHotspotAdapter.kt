package org.rfcx.audiomoth.view.deployment.guardian.connect

import android.net.wifi.ScanResult
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.item_guardian_hotspot.view.*
import org.rfcx.audiomoth.R

class GuardianHotspotAdapter(private val onHotspotClickListener: (ScanResult) -> Unit): RecyclerView.Adapter<GuardianHotspotAdapter.GuardianHotspotViewHolder>() {

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
        val hotspot = items[position]
        holder.bind(hotspot)
        holder.itemView.setOnClickListener {
//            this.onHotspotClickListener(hotspot)
        }
    }

    inner class GuardianHotspotViewHolder(itemView: View): RecyclerView.ViewHolder(itemView) {
        private val hotspotName = itemView.hotspotNameTextView
        private val deployStatus = itemView.deployStatusTextView

        fun bind(hotspot: ScanResult) {
            hotspotName.text = hotspot.SSID
            deployStatus.text = "not deployed"
            //TODO: make item feel like radiobutton but highlight instead
        }
    }

}
