package org.rfcx.companion.view.detail

import android.content.Context
import android.os.Handler
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.item_image.view.*
import org.rfcx.companion.R
import org.rfcx.companion.adapter.BaseListItem
import org.rfcx.companion.entity.SyncState
import org.rfcx.companion.extension.setDeploymentImage
import org.rfcx.companion.util.getIdToken

class DeploymentImageAdapter :
    ListAdapter<BaseListItem, RecyclerView.ViewHolder>(DeploymentImageViewDiff()) {

    private var imagesSource = arrayListOf<BaseListItem>()
    var onImageAdapterClickListener: OnImageAdapterClickListener? = null
    private var context: Context? = null

    companion object {
        const val VIEW_TYPE_IMAGE = 1
        const val VIEW_TYPE_ADD_IMAGE = 2
        const val MAX_IMAGE_SIZE = 10
    }

    fun getImageCount(): Int = if (imagesSource[imagesSource.count() - 1] is AddImageItem) imagesSource.count() - 1
    else imagesSource.count()

    fun setImages(reportImages: List<DeploymentImageView>) {
        imagesSource = arrayListOf()
        var index = 0
        reportImages.forEach {
            if (it.remotePath != null) {
                imagesSource.add(RemoteImageItem(index, it, false))
            } else {
                imagesSource.add(
                    LocalImageItem(
                        index,
                        it,
                        it.syncState == SyncState.Unsent.key
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
                Toast.makeText(context, R.string.some_photo_already_exists, Toast.LENGTH_SHORT).show()
            }

            localPathImagesForAdd.forEach {
                imagesSource.add(LocalImageItem(index, DeploymentImageView(id = 0, localPath = it, remotePath = null), true))
                index++
            }
        } else {
            if (uris.size > 1) {
                Toast.makeText(context, R.string.these_photos_already_exists, Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(context, R.string.this_photo_already_exists, Toast.LENGTH_SHORT).show()
            }
        }

        if (imagesSource.count() < MAX_IMAGE_SIZE) {
            imagesSource.add(AddImageItem())
        }
        submitList(ArrayList(imagesSource))
    }

    fun getNewAttachImage(): List<String> {
        return imagesSource.filter {
            (it is LocalImageItem && it.canDelete && it.deploymentImage.id == 0)
        }.map {
            (it as LocalImageItem).deploymentImage.localPath
        }
    }

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

    inner class ImageAdapterViewHolder(
        itemView: View,
        private val onImageAdapterClickListener: OnImageAdapterClickListener?
    ) : RecyclerView.ViewHolder(itemView) {
        private val imageView = itemView.image
        private val deleteButton = itemView.deleteImageButton
        private val syncImageView = itemView.syncImage
        private val progress = itemView.progressBarOfImageView

        fun bind(item: DeploymentImageView, canDelete: Boolean) {
            syncImageView.visibility = View.VISIBLE
            syncImageView.setImageDrawable(
                ContextCompat.getDrawable(
                    itemView.context,
                    item.syncImage
                )
            )

            val token = itemView.context.getIdToken()
            val fromServer = item.remotePath != null
            imageView.setDeploymentImage(
                url = item.remotePath ?: item.localPath,
                blur = item.syncState != SyncState.Sent.key,
                fromServer = fromServer,
                token = token,
                progressBar = progress
            )

            // handle hide syncing image view after sent in 2sec
            if (item.syncState == SyncState.Sent.key) {
                val handler = Handler()
                handler.postDelayed({
                    syncImageView.visibility = View.INVISIBLE
                }, 2000) // 2s
            }

            itemView.setOnClickListener {
                onImageAdapterClickListener?.onImageClick(item)
            }

            deleteButton.setOnClickListener {
                onImageAdapterClickListener?.onDeleteImageClick(adapterPosition, item.localPath)
            }
            if (item.id == 0) {
                deleteButton.visibility = if (canDelete) View.VISIBLE else View.INVISIBLE
            } else {
                deleteButton.visibility = View.INVISIBLE
            }
        }
    }

    inner class AddImageViewHolder(
        itemView: View,
        private val onImageAdapterClickListener: OnImageAdapterClickListener?
    ) : RecyclerView.ViewHolder(itemView) {
        init {
            itemView.setOnClickListener {
                onImageAdapterClickListener?.onAddImageClick()
            }
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder is ImageAdapterViewHolder && getItem(position) is LocalImageItem) {
            val itemImage = getItem(position) as LocalImageItem
            holder.bind(itemImage.deploymentImage, itemImage.canDelete)
        } else if (holder is ImageAdapterViewHolder && getItem(position) is RemoteImageItem) {
            val itemImage = getItem(position) as RemoteImageItem
            holder.bind(itemImage.deploymentImage, false)
        }
    }

    private class DeploymentImageViewDiff : DiffUtil.ItemCallback<BaseListItem>() {
        override fun areItemsTheSame(oldItem: BaseListItem, newItem: BaseListItem): Boolean {
            return oldItem.getItemId() == newItem.getItemId()
        }

        override fun areContentsTheSame(oldItem: BaseListItem, newItem: BaseListItem): Boolean {
            return if (newItem is org.rfcx.companion.adapter.LocalImageItem && oldItem is org.rfcx.companion.adapter.LocalImageItem) {
                (newItem.imageId == oldItem.imageId && newItem.localPath == oldItem.localPath)
            } else newItem is org.rfcx.companion.adapter.RemoteImageItem && oldItem is org.rfcx.companion.adapter.RemoteImageItem
        }
    }
}

data class LocalImageItem(var imageId: Int, val deploymentImage: DeploymentImageView, val canDelete: Boolean) :
    BaseListItem {
    override fun getItemId(): Int = imageId
}

data class RemoteImageItem(var imageId: Int, val deploymentImage: DeploymentImageView, val canDelete: Boolean) :
    BaseListItem {
    override fun getItemId(): Int = imageId
}

data class AddImageItem(val any: Any? = null) : BaseListItem {
    override fun getItemId(): Int = -11
}

interface OnImageAdapterClickListener {
    fun onAddImageClick()
    fun onImageClick(deploymentImageView: DeploymentImageView)
    fun onDeleteImageClick(position: Int, imagePath: String)
}
