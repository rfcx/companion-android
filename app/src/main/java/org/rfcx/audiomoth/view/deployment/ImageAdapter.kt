package org.rfcx.audiomoth.view.deployment

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
import kotlinx.android.synthetic.main.item_add_image.view.*
import org.rfcx.audiomoth.R
import org.rfcx.audiomoth.adapter.AddImageItem
import org.rfcx.audiomoth.adapter.BaseListItem
import org.rfcx.audiomoth.adapter.LocalImageItem
import org.rfcx.audiomoth.adapter.RemoteImageItem
import org.rfcx.audiomoth.entity.DeploymentImage

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
        if (imagesSource.count() < MAX_IMAGE_SIZE) {
            imagesSource.add(AddImageItem())
        }

        submitList(ArrayList(imagesSource))
    }

    fun removeAt(index: Int) {
        if (getItem(imagesSource.count() - 1) is AddImageItem) {
            imagesSource.removeAt(imagesSource.count() - 1)
        }

        imagesSource.removeAt(index)

        if (imagesSource.count() < MAX_IMAGE_SIZE) {
            imagesSource.add(AddImageItem())
        }
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

        if (getItem(imagesSource.count() - 1) is AddImageItem) {
            imagesSource.removeAt(imagesSource.count() - 1)
        }
        var index: Int = if (imagesSource.isEmpty()) 0 else {
            imagesSource[imagesSource.count() - 1].getItemId() + 1
        }

        if (localPathImagesForAdd.isNotEmpty()) {
            if (localPathImagesForAdd.size != uris.size) {
                Toast.makeText(context, R.string.some_photo_already_exists, Toast.LENGTH_SHORT)
                    .show()
            }

            localPathImagesForAdd.forEach {
                imagesSource.add(LocalImageItem(index, it, true))
                index++
            }
        } else {
            if (uris.size > 1) {
                Toast.makeText(context, R.string.these_photos_already_exists, Toast.LENGTH_SHORT)
                    .show()
            } else {
                Toast.makeText(context, R.string.this_photo_already_exists, Toast.LENGTH_SHORT)
                    .show()
            }
        }

        if (imagesSource.count() < MAX_IMAGE_SIZE) {
            imagesSource.add(AddImageItem())
        }
        submitList(ArrayList(imagesSource))
    }

    fun getImageCount(): Int =
        if (imagesSource[imagesSource.count() - 1] is AddImageItem) imagesSource.count() - 1
        else imagesSource.count()

    override fun getItemViewType(position: Int): Int {
        return when (getItem(position)) {
            is LocalImageItem -> VIEW_TYPE_IMAGE
            is RemoteImageItem -> VIEW_TYPE_IMAGE
            is AddImageItem -> VIEW_TYPE_ADD_IMAGE
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
            VIEW_TYPE_ADD_IMAGE -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_add_image, parent, false)
                AddImageViewHolder(view, onImageAdapterClickListener)
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
                .placeholder(R.drawable.bg_grey_light)
                .error(R.drawable.bg_grey_light)
                .into(imageView)

            deleteButton.setOnClickListener {
                onImageAdapterClickListener?.onDeleteImageClick(adapterPosition, imagePath)
            }
            deleteButton.visibility = if (canDelete) View.VISIBLE else View.INVISIBLE
        }
    }

    inner class AddImageViewHolder(
        itemView: View,
        private val onImageAdapterClickListener: OnImageAdapterClickListener?
    ) : RecyclerView.ViewHolder(itemView) {
        init {
            itemView.addItemImage.setOnClickListener {
                onImageAdapterClickListener?.onAddImageClick()
            }
        }
    }

    class ImageAdapterDiffUtil : DiffUtil.ItemCallback<BaseListItem>() {
        override fun areItemsTheSame(oldItem: BaseListItem, newItem: BaseListItem): Boolean {
            return oldItem.getItemId() == newItem.getItemId()
        }

        override fun areContentsTheSame(oldItem: BaseListItem, newItem: BaseListItem): Boolean {
            return if (newItem is LocalImageItem && oldItem is LocalImageItem) {
                (newItem.imageId == oldItem.imageId && newItem.localPath == oldItem.localPath)
            } else if (newItem is RemoteImageItem && oldItem is RemoteImageItem) {
                (newItem.imageId == oldItem.imageId && newItem.remotePath == oldItem.remotePath)
            } else newItem is AddImageItem && oldItem is AddImageItem
        }
    }

    companion object {
        const val VIEW_TYPE_IMAGE = 1
        const val VIEW_TYPE_ADD_IMAGE = 2
        const val MAX_IMAGE_SIZE = 5
    }
}

interface OnImageAdapterClickListener {
    fun onAddImageClick()
    fun onDeleteImageClick(position: Int, imagePath: String)
}