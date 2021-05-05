package org.rfcx.companion.view.map

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.item_site.view.*
import org.rfcx.companion.R
import org.rfcx.companion.entity.Locate
import org.rfcx.companion.util.setFormatLabel
import org.rfcx.companion.util.toTimeSinceStringAlternativeTimeAgo
import org.rfcx.companion.view.deployment.locate.SiteWithLastDeploymentItem

class SiteAdapter(private val itemClickListener: (Locate, Boolean) -> Unit) :
    RecyclerView.Adapter<SiteAdapter.SiteAdapterViewHolder>() {
    var items: ArrayList<SiteWithLastDeploymentItem> = arrayListOf()
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    var isNewSite = false

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): SiteAdapter.SiteAdapterViewHolder {
        val view =
            LayoutInflater.from(parent.context).inflate(R.layout.item_site, parent, false)
        return SiteAdapterViewHolder(view)
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: SiteAdapter.SiteAdapterViewHolder, position: Int) {
        val site = items[position]
        holder.bind(site)
        holder.itemView.setOnClickListener {
            this.itemClickListener(site.locate, isNewSite)
        }
    }

    fun setFilter(newList: ArrayList<SiteWithLastDeploymentItem>?) {
        items = arrayListOf()
        items.addAll(newList ?: arrayListOf())
        notifyDataSetChanged()
    }

    inner class SiteAdapterViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val siteNameTextView = itemView.siteNameTextView
        private val detailTextView = itemView.detailTextView
        private val distanceTextView = itemView.distanceTextView
        private val iconAddImageView = itemView.iconAddImageView

        fun bind(site: SiteWithLastDeploymentItem) {
            siteNameTextView.text = if (isNewSite) itemView.context.getString(
                R.string.create_site,
                site.locate.name
            ) else site.locate.name
            detailTextView.text = site.date?.toTimeSinceStringAlternativeTimeAgo(itemView.context)
                ?: itemView.context.getString(R.string.no_deployments)
            if (site.distance != null) {
                distanceTextView.visibility = View.VISIBLE
                distanceTextView.text = site.distance.setFormatLabel()
            } else {
                distanceTextView.visibility = View.GONE
            }
            setDistanceAndIconAdd(site.locate.latitude == 0.0)
        }

        private fun setDistanceAndIconAdd(boolean: Boolean) {
            distanceTextView.visibility = if (boolean) View.GONE else View.VISIBLE
            detailTextView.visibility = if (boolean) View.GONE else View.VISIBLE
            iconAddImageView.visibility = if (boolean) View.VISIBLE else View.GONE
        }
    }
}
