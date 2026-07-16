package org.rfcx.companion.view.detail

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import org.rfcx.companion.databinding.ItemDisplayImageBinding
import org.rfcx.companion.extension.setDeploymentImage
import org.rfcx.companion.util.getIdToken

class DisplayImageAdapter(private val imageList: List<String>, private val context: Context) :
    RecyclerView.Adapter<DisplayImageAdapter.DisplayImageViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DisplayImageViewHolder {
        val binding = ItemDisplayImageBinding.inflate(LayoutInflater.from(context), parent, false)
        return DisplayImageViewHolder(binding)
    }

    override fun onBindViewHolder(holder: DisplayImageViewHolder, position: Int) {
        holder.bind(imageList[position])
    }

    override fun getItemCount(): Int = imageList.size

    inner class DisplayImageViewHolder(binding: ItemDisplayImageBinding) : RecyclerView.ViewHolder(binding.root) {
        private val imageView = binding.displayImage
        private val progressBar = binding.progressBarOfImageView

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
