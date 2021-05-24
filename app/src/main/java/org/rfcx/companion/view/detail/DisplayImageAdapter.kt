package org.rfcx.companion.view.detail

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.item_display_image.view.*
import org.rfcx.companion.R
import org.rfcx.companion.extension.setDeploymentImage
import org.rfcx.companion.util.getIdToken

class DisplayImageAdapter(private val imageList: List<String>, private val context: Context) :
    RecyclerView.Adapter<DisplayImageAdapter.DisplayImageViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DisplayImageViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.item_display_image, parent, false)
        return DisplayImageViewHolder(view)
    }

    override fun onBindViewHolder(holder: DisplayImageViewHolder, position: Int) {
        holder.bind(imageList[position])
    }

    override fun getItemCount(): Int = imageList.size


    inner class DisplayImageViewHolder(itemView: View): RecyclerView.ViewHolder(itemView) {
        private val imageView = itemView.displayImage
        private val progressBar = itemView.progressBarOfImageView

        fun bind(item: String) {
            val token = context.getIdToken()
            val fromServer = !item.startsWith("file")
            imageView.setDeploymentImage(
                url = item,
                blur = false,
                fromServer = fromServer,
                token = token,
                progressBar = progressBar
            )
        }
    }

}
