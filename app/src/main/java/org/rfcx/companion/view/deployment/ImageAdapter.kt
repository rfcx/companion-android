package org.rfcx.companion.view.deployment

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import kotlinx.android.synthetic.main.item_image.view.*
import org.rfcx.companion.R
import org.rfcx.companion.adapter.BaseListItem
import org.rfcx.companion.adapter.LocalImageItem
import org.rfcx.companion.adapter.RemoteImageItem
import org.rfcx.companion.entity.DeploymentImage

class ImageAdapter : ListAdapter<BaseListItem, RecyclerView.ViewHolder>(ImageAdapterDiffUtil()) {
    var onImageAdapterClickListener: OnImageAdapterClickListener? = null
    private var context: Context? = null
    private var imagesSource = arrayListOf<BaseListItem>()

    fun setImages(images: List<DeploymentImage>) {
        imagesSource = arrayListOf()
        var index = 0
        images.forEach {
            if (it.remotePath != null) {
                imagesSource.add(RemoteImageItem(index, it.remotePath!!, false))
            } else {
                imagesSource.add(
                    LocalImageItem(
                        index,
                        it.localPath,
                        false
                    )
                )
            }
            index++
        }
        submitList(ArrayList(imagesSource))
    }

    fun removeAt(index: Int) {
        imagesSource.removeAt(index)
        submitList(ArrayList(imagesSource))
    }

    fun getNewAttachImage(): List<String> {
        return imagesSource.filter {
            (it is LocalImageItem && it.canDelete)
        }.map {
            (it as LocalImageItem).localPath
        }
    }

    fun addImages(uris: List<String>) {
        val allLocalPathImages = getNewAttachImage() + uris
        val groups = allLocalPathImages.groupBy { it }
        val localPathImages = groups.filter { it.value.size < 2 }
        val localPathImagesForAdd = ArrayList<String>()

        localPathImages.forEach {
            if (it.key !in getNewAttachImage()) {
                localPathImagesForAdd.add(it.key)
            }
        }

        var index: Int = if (imagesSource.isEmpty()) 0 else {
            imagesSource[imagesSource.count() - 1].getItemId() + 1
        }

        if (localPathImagesForAdd.isNotEmpty()) {
            if (context != null) {
                if (localPathImagesForAdd.size != uris.size) {
                    Toast.makeText(context, R.string.some_photo_already_exists, Toast.LENGTH_SHORT)
                        .show()
                }
            }

            localPathImagesForAdd.forEach {
                imagesSource.add(LocalImageItem(index, it, true))
                index++
            }
        } else {
            if (context != null) {
                if (uris.size > 1) {
                    Toast.makeText(
                        context,
                        R.string.these_photos_already_exists,
                        Toast.LENGTH_SHORT
                    )
                        .show()
                } else {
                    Toast.makeText(context, R.string.this_photo_already_exists, Toast.LENGTH_SHORT)
                        .show()
                }
            }
        }
        submitList(ArrayList(imagesSource))
    }

    fun getImageCount(): Int = imagesSource.count()

    override fun getItemViewType(position: Int): Int {
        return when (getItem(position)) {
            is LocalImageItem -> VIEW_TYPE_IMAGE
            is RemoteImageItem -> VIEW_TYPE_IMAGE
            else -> throw IllegalStateException("Item class not found ${getItem(position)::class.java.simpleName}")
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        context = parent.context

        return when (viewType) {
            VIEW_TYPE_IMAGE -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_image, parent, false)
                ImageAdapterViewHolder(view, onImageAdapterClickListener)
            }
            else -> throw IllegalAccessException("View type $viewType not found.")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder is ImageAdapterViewHolder && getItem(position) is LocalImageItem) {
            val itemImage = getItem(position) as LocalImageItem
            holder.bind(itemImage.localPath, itemImage.canDelete)
        } else if (holder is ImageAdapterViewHolder && getItem(position) is RemoteImageItem) {
            val itemImage = getItem(position) as RemoteImageItem
            holder.bind(itemImage.remotePath, false)
        }
    }

    inner class ImageAdapterViewHolder(
        itemView: View,
        private val onImageAdapterClickListener: OnImageAdapterClickListener?
    ) : RecyclerView.ViewHolder(itemView) {
        private val imageView = itemView.image
        private val deleteButton = itemView.deleteImageButton

        fun bind(imagePath: String, canDelete: Boolean) {
            Glide.with(itemView.context)
                .load(imagePath)
                .placeholder(R.drawable.bg_placeholder_light)
                .error(R.drawable.bg_placeholder_light)
                .into(imageView)

            itemView.setOnClickListener {
                onImageAdapterClickListener?.onImageClick(imagePath)
            }

            deleteButton.setOnClickListener {
                onImageAdapterClickListener?.onDeleteImageClick(adapterPosition, imagePath)
            }
            deleteButton.visibility = if (canDelete) View.VISIBLE else View.INVISIBLE
        }
    }

    inner class AddImageViewHolder(
        itemView: View,
        private val onImageAdapterClickListener: OnImageAdapterClickListener?
    ) : RecyclerView.ViewHolder(itemView)

    class ImageAdapterDiffUtil : DiffUtil.ItemCallback<BaseListItem>() {
        override fun areItemsTheSame(oldItem: BaseListItem, newItem: BaseListItem): Boolean {
            return oldItem.getItemId() == newItem.getItemId()
        }

        override fun areContentsTheSame(oldItem: BaseListItem, newItem: BaseListItem): Boolean {
            return if (newItem is LocalImageItem && oldItem is LocalImageItem) {
                (newItem.imageId == oldItem.imageId && newItem.localPath == oldItem.localPath)
            } else newItem is RemoteImageItem && oldItem is RemoteImageItem
        }
    }

    companion object {
        const val VIEW_TYPE_IMAGE = 1
        const val MAX_IMAGE_SIZE = 10
    }
}

interface OnImageAdapterClickListener {
    fun onDeleteImageClick(position: Int, imagePath: String)
    fun onImageClick(imagePath: String)
}
