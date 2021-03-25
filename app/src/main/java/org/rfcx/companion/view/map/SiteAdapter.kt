package org.rfcx.companion.view.map

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.item_site.view.*
import org.rfcx.companion.R
import org.rfcx.companion.entity.Locate
import org.rfcx.companion.util.toDateString

class SiteAdapter(private val itemClickListener: (Locate) -> Unit) :
    RecyclerView.Adapter<SiteAdapter.SiteAdapterViewHolder>() {
    var items: ArrayList<Locate> = arrayListOf()
        set(value) {
            field = value
            notifyDataSetChanged()
        }

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
            this.itemClickListener(site)
        }
    }

    fun setFilter(newList: ArrayList<Locate>?) {
        items = arrayListOf()
        items.addAll(newList ?: arrayListOf())
        notifyDataSetChanged()
    }

    inner class SiteAdapterViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val siteNameTextView = itemView.siteNameTextView
        private val detailTextView = itemView.detailTextView

        fun bind(site: Locate) {
            siteNameTextView.text = site.name
            detailTextView.text = site.updatedAt?.toDateString()
        }
    }
}
