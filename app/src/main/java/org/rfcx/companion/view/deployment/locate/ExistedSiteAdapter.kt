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
    var items: List<Locate> = arrayListOf()
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
        val locate = items[position]
        holder.bind(locate)
        holder.itemView.setOnClickListener {
            this.itemClickListener(locate)
        }
    }

    inner class ExistedSiteAdapterViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val siteTextView = itemView.siteTextView

        fun bind(locate: Locate) {
            siteTextView.text = locate.name
        }
    }
}
