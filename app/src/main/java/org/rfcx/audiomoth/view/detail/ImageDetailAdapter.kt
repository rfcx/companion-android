package org.rfcx.audiomoth.view.detail

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import kotlinx.android.synthetic.main.adapter_image.view.*
import org.rfcx.audiomoth.R

class ImageDetailAdapter : RecyclerView.Adapter<ImageDetailAdapter.ImageDetailAdapterViewHolder>() {
    var items: ArrayList<String> = arrayListOf()
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ImageDetailAdapterViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val view = inflater.inflate(R.layout.adapter_image, parent, false)
        return ImageDetailAdapterViewHolder(view)
    }

    override fun onBindViewHolder(holder: ImageDetailAdapterViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    inner class ImageDetailAdapterViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        fun bind(imagePath: String) {
            Glide.with(itemView.image)
                .load(imagePath)
                .placeholder(R.drawable.bg_grey_light)
                .error(R.drawable.bg_grey_light)
                .into(itemView.image)

            itemView.deleteImageButton.visibility = View.GONE
        }
    }
}
