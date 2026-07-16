package org.rfcx.companion.view.profile

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Toast
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import org.rfcx.companion.R
import org.rfcx.companion.adapter.BaseListItem
import org.rfcx.companion.databinding.ItemFeedbackImageBinding

class FeedbackImageAdapter :
    ListAdapter<BaseListItem, RecyclerView.ViewHolder>(FeedbackImageAdapterDiffUtil()) {

    companion object {
        const val MAX_IMAGE_SIZE = 5
        const val VIEW_TYPE_IMAGE = 1
        const val VIEW_TYPE_ADD_IMAGE = 2
    }

    var onFeedbackImageAdapterClickListener: OnFeedbackImageAdapterClickListener? = null
    private var context: Context? = null
    private var imagesSource = arrayListOf<BaseListItem>()

    fun addImages(uris: List<String>) {
        val allLocalPathImages = getLocalImageItem() + uris
        val groups = allLocalPathImages.groupBy { it }
        val localPathImages = groups.filter { it.value.size < 2 }
        val localPathImagesForAdd = ArrayList<String>()

        localPathImages.forEach {
            if (it.key !in getLocalImageItem()) {
                localPathImagesForAdd.add(it.key)
            }
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

        submitList(ArrayList(imagesSource))
        onFeedbackImageAdapterClickListener?.pathListArray(imagesSource)
    }

    fun removeAt(index: Int) {
        if (getItem(imagesSource.count() - 1) is AddImageItem) {
            imagesSource.removeAt(imagesSource.count() - 1)
        }
        imagesSource.removeAt(index)
        submitList(ArrayList(imagesSource))
        onFeedbackImageAdapterClickListener?.pathListArray(imagesSource)
    }

    fun getImageCount(): Int {
        return imagesSource.count()
    }

    private fun getLocalImageItem(): List<String> {
        return imagesSource.filter {
            (it is LocalImageItem && it.canDelete)
        }.map {
            (it as LocalImageItem).localPath
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        context = parent.context
        val binding = ItemFeedbackImageBinding
            .inflate(LayoutInflater.from(parent.context), parent, false)
        return FeedbackImageAdapterViewHolder(binding, onFeedbackImageAdapterClickListener)
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder is FeedbackImageAdapterViewHolder && getItem(position) is LocalImageItem) {
            val itemImage = getItem(position) as LocalImageItem
            holder.bind(itemImage.localPath)
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

    inner class FeedbackImageAdapterViewHolder(
        private val binding: ItemFeedbackImageBinding,
        private val onFeedbackImageAdapterClickListener: OnFeedbackImageAdapterClickListener?
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(imagePath: String) {

            val text: List<String>? = imagePath.split("/")
            binding.nameImageTextView.text = text?.get(text.size - 1) ?: ""

            Glide.with(binding.imageFeedbackImageView)
                .load(imagePath)
                .placeholder(R.drawable.bg_placeholder_light)
                .error(R.drawable.bg_placeholder_light)
                .into(binding.imageFeedbackImageView)

            binding.deleteImageFeedbackButton.setOnClickListener {
                onFeedbackImageAdapterClickListener?.onDeleteImageClick(adapterPosition)
            }
        }
    }

    class FeedbackImageAdapterDiffUtil : DiffUtil.ItemCallback<BaseListItem>() {
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
}

data class LocalImageItem(var imageId: Int, val localPath: String, val canDelete: Boolean) :
    BaseListItem {
    override fun getItemId(): Int = imageId
}

data class RemoteImageItem(var imageId: Int, val remotePath: String, val canDelete: Boolean) :
    BaseListItem {
    override fun getItemId(): Int = imageId
}

data class AddImageItem(val any: Any? = null) : BaseListItem {
    override fun getItemId(): Int = -11
}
