package org.rfcx.companion.view.deployment

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import androidx.appcompat.widget.AppCompatButton
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import kotlinx.android.synthetic.main.item_photo_advise.view.*
import org.rfcx.companion.R

class ImageAdapter(private val imageClickListener: ImageClickListener, private val thumbnails: List<String>) :
    RecyclerView.Adapter<ImageAdapter.ImageAdapterViewHolder>() {
    private var imageItems = arrayListOf<Image>()
    private var currentPosition = -1
    private var currentType: ImageType = ImageType.NORMAL

    companion object {
        private const val MAX_IMAGES = 10
    }

    fun setPlaceHolders(type: List<String>) {
        type.forEachIndexed { index, it ->
            imageItems.add(Image(index + 1, it, ImageType.NORMAL, null))
        }
        // For other images that out of type scoped
        if (imageItems.size < MAX_IMAGES) {
            imageItems.add(Image(imageItems.size + 1, ImageType.OTHER.value, ImageType.OTHER, null))
        }
        notifyDataSetChanged()
    }

    fun updateImagesFromSavedImages(images: List<Image>) {
        images.map { it.copy() }.forEach {
            imageItems.add(it)
        }
        notifyDataSetChanged()
    }

    fun updateTakeOrChooseImage(path: String) {
        if (currentPosition == -1) return
        if (currentType == ImageType.OTHER) {
            if (itemCount == MAX_IMAGES) {
                imageItems.removeLast()
            }
            imageItems.add(itemCount - 1, Image(itemCount, ImageType.OTHER.value, ImageType.OTHER, path))
        } else {
            imageItems[currentPosition].path = path
        }
        notifyDataSetChanged()
    }

    fun removeImage(image: Image) {
        if (currentPosition == -1) return
        if (image.type == ImageType.OTHER) {
            if (getAvailableImagesLeft() == 0) {
                imageItems.add(Image(currentPosition, ImageType.OTHER.value, ImageType.OTHER, null))
            }
            imageItems.remove(image)
        } else {
            imageItems[currentPosition].path = null
        }
        notifyDataSetChanged()
    }

    private fun getOtherCount() = imageItems.filter { it.type == ImageType.OTHER && it.path != null }.size
    private fun getNormalCount() = imageItems.filter { it.type == ImageType.NORMAL }.size
    private fun getAvailableImagesLeft(): Int {
        return (MAX_IMAGES - (getOtherCount() + getNormalCount()))
    }

    fun getCurrentImagePaths() = imageItems

    fun getMissingImages(): List<Image> {
        val missing = imageItems.filter { it.path == null && it.type != ImageType.OTHER }
        if (getExistingImages().find { it.id == 9 } != null) return missing.filter { it.id != 10 }
        if (getExistingImages().find { it.id == 10 } != null) return missing.filter { it.id != 9 }
        return missing
    }
    fun getExistingImages() = imageItems.filter { it.path != null }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ImageAdapterViewHolder {
        val view =
            LayoutInflater.from(parent.context).inflate(R.layout.item_photo_advise, parent, false)
        return ImageAdapterViewHolder(view)
    }

    override fun onBindViewHolder(holder: ImageAdapterViewHolder, position: Int) {
        holder.bind(imageItems[position])

        holder.placeHolderButton.setOnClickListener {
            currentPosition = holder.adapterPosition
            currentType = imageItems[currentPosition].type
            imageClickListener.onPlaceHolderClick(currentPosition)
        }

        holder.imageView.setOnClickListener {
            currentPosition = holder.adapterPosition
            imageClickListener.onImageClick(imageItems[currentPosition])
        }
        holder.deleteButton.setOnClickListener {
            currentPosition = holder.adapterPosition
            imageClickListener.onDeleteClick(imageItems[currentPosition])
        }
    }

    inner class ImageAdapterViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val placeHolderButton: AppCompatButton = itemView.placeHolderButton
        val imageView: ImageView = itemView.image
        val deleteButton: ImageButton = itemView.deleteImageButton

        fun bind(image: Image) {
            if (image.path == null) {
                placeHolderButton.text = image.name
                imageView.visibility = View.GONE
                placeHolderButton.visibility = View.VISIBLE
                deleteButton.visibility = View.GONE
                placeHolderButton.apply {
                    val example = thumbnails.getOrNull(adapterPosition) ?: thumbnails[thumbnails.size - 1]
                    val id = this.context.resources.getIdentifier("${example}_tbn", "drawable", this.context.packageName)
                    if (id != 0) {
                        ContextCompat.getDrawable(this.context, id)?.let {
                            val drawable = it.mutate()
                            drawable.alpha = 100
                            placeHolderButton.background = drawable
                        }
                    }
                }
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
    val id: Int,
    val name: String,
    val type: ImageType,
    var path: String?
)

enum class ImageType(val value: String) {
    NORMAL("normal"),
    OTHER("other"),
}

interface ImageClickListener {
    fun onPlaceHolderClick(position: Int)
    fun onImageClick(image: Image)
    fun onDeleteClick(image: Image)
}
