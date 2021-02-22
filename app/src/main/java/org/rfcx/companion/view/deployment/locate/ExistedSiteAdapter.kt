package org.rfcx.companion.view.deployment.locate

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.item_existed_site.view.*
import org.rfcx.companion.R
import org.rfcx.companion.entity.Locate

class ExistedSiteAdapter(private val itemClickListener: (Locate) -> Unit) :
    RecyclerView.Adapter<ExistedSiteAdapter.ExistedSiteAdapterViewHolder>() {
    var items: List<SiteItem> = arrayListOf()
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ExistedSiteAdapterViewHolder {
        val view =
            LayoutInflater.from(parent.context).inflate(R.layout.item_existed_site, parent, false)
        return ExistedSiteAdapterViewHolder(view)
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: ExistedSiteAdapterViewHolder, position: Int) {
        val site = items[position]
        holder.bind(site)
        holder.itemView.setOnClickListener {
            this.itemClickListener(site.locate)
        }
    }

    inner class ExistedSiteAdapterViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val siteTextView = itemView.siteTextView
        private val distanceTextView = itemView.distanceTextView
        private val iconAddImageView = itemView.iconAddImageView

        fun bind(site: SiteItem) {
            siteTextView.text = site.locate.name
            distanceTextView.visibility =
                if (site.locate.name == itemView.context.getString(R.string.create_new_site)) View.GONE else View.VISIBLE
            distanceTextView.text = "${String.format("%.2f", site.distance)} m"
            iconAddImageView.visibility =
                if (site.locate.name == itemView.context.getString(R.string.create_new_site)) View.VISIBLE else View.GONE
        }
    }
}
