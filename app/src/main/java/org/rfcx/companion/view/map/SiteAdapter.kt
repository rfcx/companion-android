package org.rfcx.companion.view.map

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import org.rfcx.companion.R
import org.rfcx.companion.databinding.ItemSiteBinding
import org.rfcx.companion.entity.Stream
import org.rfcx.companion.util.setFormatLabel
import org.rfcx.companion.util.toTimeSinceStringAlternativeTimeAgo
import org.rfcx.companion.view.deployment.locate.SiteWithLastDeploymentItem

class SiteAdapter(private val itemClickListener: (Stream, Boolean) -> Unit) :
    RecyclerView.Adapter<SiteAdapter.SiteAdapterViewHolder>() {
    var items: List<SiteWithLastDeploymentItem> = listOf()
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): SiteAdapter.SiteAdapterViewHolder {
        val binding =
            ItemSiteBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return SiteAdapterViewHolder(binding)
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: SiteAdapter.SiteAdapterViewHolder, position: Int) {
        val site = items[position]
        holder.bind(site)
        holder.itemView.setOnClickListener {
            this.itemClickListener(site.stream, site.stream.id == -1)
        }
    }

    fun setFilter(newList: List<SiteWithLastDeploymentItem>?) {
        items = newList ?: listOf()
        notifyDataSetChanged()
    }

    inner class SiteAdapterViewHolder(binding: ItemSiteBinding) : RecyclerView.ViewHolder(binding.root) {
        private val siteNameTextView = binding.siteNameTextView
        private val createdSiteNameTextView = binding.createdSiteNameTextView
        private val detailTextView = binding.detailTextView
        private val distanceTextView = binding.distanceTextView
        private val iconAddImageView = binding.iconAddImageView

        fun bind(site: SiteWithLastDeploymentItem) {
            createdSiteNameTextView.text =
                itemView.context.getString(R.string.create_site, site.stream.name)
            siteNameTextView.text = site.stream.name

            createdSiteNameTextView.visibility =
                if (site.stream.id == -1) View.VISIBLE else View.GONE
            siteNameTextView.visibility = if (site.stream.id != -1) View.VISIBLE else View.GONE

            detailTextView.text = site.date?.toTimeSinceStringAlternativeTimeAgo(itemView.context)
                ?: itemView.context.getString(R.string.no_deployments)
            if (site.distance != null) {
                distanceTextView.visibility = View.VISIBLE
                distanceTextView.text = site.distance.setFormatLabel()
            } else {
                distanceTextView.visibility = View.GONE
            }
            setDistanceAndIconAdd(site.stream.id == -1)
        }

        private fun setDistanceAndIconAdd(boolean: Boolean) {
            distanceTextView.visibility = if (boolean) View.GONE else View.VISIBLE
            detailTextView.visibility = if (boolean) View.GONE else View.VISIBLE
            iconAddImageView.visibility = if (boolean) View.VISIBLE else View.GONE
        }
    }
}
