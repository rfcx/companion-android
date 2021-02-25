package org.rfcx.companion.view.detail

import android.os.Handler
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.item_detail_image.view.*
import org.rfcx.companion.R
import org.rfcx.companion.entity.SyncState
import org.rfcx.companion.extension.setDeploymentImage
import org.rfcx.companion.util.getIdToken

class DeploymentImageAdapter(private val itemClickListener: (DeploymentImageView) -> Unit) : ListAdapter<DeploymentImageView,
        DeploymentImageAdapter.ImageDetailViewHolder>(DeploymentImageViewDiff()) {

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ImageDetailViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val view = inflater.inflate(R.layout.item_detail_image, parent, false)
        return ImageDetailViewHolder(view)
    }

    private class DeploymentImageViewDiff : DiffUtil.ItemCallback<DeploymentImageView>() {
        override fun areItemsTheSame(
            oldItem: DeploymentImageView,
            newItem: DeploymentImageView
        ): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(
            oldItem: DeploymentImageView,
            newItem: DeploymentImageView
        ): Boolean {
            return oldItem.localPath == newItem.localPath &&
                    oldItem.remotePath == newItem.remotePath &&
                    oldItem.syncState == newItem.syncState
        }
    }

    override fun onBindViewHolder(holder: ImageDetailViewHolder, position: Int) {
        holder.bind(getItem(position))
        holder.itemView.setOnClickListener {
            this.itemClickListener(getItem(position))
        }
    }

    inner class ImageDetailViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val imageView = itemView.imageView
        private val syncImageView = itemView.syncImageView

        fun bind(item: DeploymentImageView) {
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
                token = token
            )

            // handle hide syncing image view after sent in 2sec
            if (item.syncState == SyncState.Sent.key) {
                val handler = Handler()
                handler.postDelayed({
                    syncImageView.visibility = View.INVISIBLE
                }, 2000) // 2s
            }
        }
    }
}
