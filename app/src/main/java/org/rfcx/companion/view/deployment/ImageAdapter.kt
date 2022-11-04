package org.rfcx.companion.view.deployment

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import androidx.appcompat.widget.AppCompatButton
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import kotlinx.android.synthetic.main.item_photo_advise.view.*
import org.rfcx.companion.R

class ImageAdapter(private val imageClickListener: ImageClickListener) :
    RecyclerView.Adapter<ImageAdapter.ImageAdapterViewHolder>() {
    private var imageItems = arrayListOf<Image>()
    private var currentPosition = -1

    fun setPlaceHolders(type: List<String>) {
        type.forEach {
            imageItems.add(Image(it, null))
        }
        notifyDataSetChanged()
    }

    fun updateImagesFromSavedImages(images: List<Image>) {
        images.forEach {
            imageItems.add(it)
        }
        notifyDataSetChanged()
    }

    fun updateTakeOrChooseImage(path: String) {
        if (currentPosition == -1) return
        imageItems[currentPosition].path = path
        notifyDataSetChanged()
    }

    fun removeImage() {
        if (currentPosition == -1) return
        imageItems[currentPosition].path = null
        notifyDataSetChanged()
    }

    fun getCurrentImagePaths() = imageItems

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ImageAdapter.ImageAdapterViewHolder {
        val view =
            LayoutInflater.from(parent.context).inflate(R.layout.item_photo_advise, parent, false)
        return ImageAdapterViewHolder(view)
    }

    override fun onBindViewHolder(holder: ImageAdapter.ImageAdapterViewHolder, position: Int) {
        holder.bind(imageItems[position])

        holder.placeHolderButton.setOnClickListener {
            currentPosition = position
            imageClickListener.onPlaceHolderClick(position)
        }
        holder.imageView.setOnClickListener {
            currentPosition = position
            imageClickListener.onImageClick(imageItems[position].path)
        }
        holder.deleteButton.setOnClickListener {
            currentPosition = position
            imageClickListener.onDeleteClick()
        }
    }

    inner class ImageAdapterViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val placeHolderButton: AppCompatButton = itemView.placeHolderButton
        val imageView: ImageView = itemView.image
        val deleteButton: ImageButton = itemView.deleteImageButton

        fun bind(image: Image) {
            if (image.path == null) {
                placeHolderButton.text = image.type
                imageView.visibility = View.GONE
                placeHolderButton.visibility = View.VISIBLE
                deleteButton.visibility = View.GONE
            } else {
                Glide.with(itemView.context)
                    .load(image.path)
                    .placeholder(R.drawable.bg_placeholder_light)
                    .error(R.drawable.bg_placeholder_light)
                    .into(imageView)
                imageView.visibility = View.VISIBLE
                placeHolderButton.visibility = View.GONE
                deleteButton.visibility = View.VISIBLE
            }
        }
    }

    override fun getItemCount() = imageItems.size
}

data class Image(
    val type: String,
    var path: String?
)

interface ImageClickListener {
    fun onPlaceHolderClick(position: Int)
    fun onImageClick(path: String?)
    fun onDeleteClick()
}
