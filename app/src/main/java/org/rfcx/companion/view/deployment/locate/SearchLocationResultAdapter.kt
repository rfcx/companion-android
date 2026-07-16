package org.rfcx.companion.view.deployment.locate

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.annotation.DrawableRes
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import org.rfcx.companion.adapter.BaseListItem
import org.rfcx.companion.databinding.ItemSearchLocationResultBinding
import org.rfcx.companion.databinding.ItemSearchLocationResultErrorBinding
import org.rfcx.companion.util.latitudeCoordinates
import org.rfcx.companion.util.longitudeCoordinates

class SearchLocationResultAdapter :
    ListAdapter<BaseListItem, RecyclerView.ViewHolder>(
        SearchLocationResultDiffCallback()
    ) {

    var onItemClick: ((position: Int, latitude: Double, longitude: Double, placeName: String) -> Unit)? =
        null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            SearchLocationViewHolder.itemViewType -> SearchLocationViewHolder.create(
                parent,
                onItemClick
            )
            SearchLocationErrorViewHolder.itemViewType -> SearchLocationErrorViewHolder.create(
                parent
            )
            else -> throw IllegalStateException("View type '$viewType' miss match on SearchLocationResultAdapter")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is SearchLocationViewHolder -> holder.bind(getItem(position) as SearchResult)
            is SearchLocationErrorViewHolder -> holder.bind(getItem(position) as SearchResultError)
        }
    }

    override fun getItemViewType(position: Int): Int {
        return when (getItem(position)) {
            is SearchResult -> SearchLocationViewHolder.itemViewType
            is SearchResultError -> SearchLocationErrorViewHolder.itemViewType
            else -> super.getItemViewType(position)
        }
    }

    fun showError(error: SearchResultError) {
        submitList(listOf(error))
    }
}

class SearchLocationResultDiffCallback : DiffUtil.ItemCallback<BaseListItem>() {
    override fun areItemsTheSame(oldItem: BaseListItem, newItem: BaseListItem): Boolean {
        return when {
            oldItem is SearchResult && newItem is SearchResult -> {
                oldItem.id == newItem.id
            }
            else -> oldItem.getItemId() == newItem.getItemId()
        }
    }

    override fun areContentsTheSame(oldItem: BaseListItem, newItem: BaseListItem): Boolean {
        return when {
            oldItem is SearchResult && newItem is SearchResult -> {
                oldItem.placeName == newItem.placeName &&
                    oldItem.latitude == newItem.latitude &&
                    oldItem.longitude == newItem.longitude
            }

            oldItem is SearchResultError && newItem is SearchResultError -> {
                oldItem.title == newItem.title &&
                    oldItem.message == newItem.message &&
                    oldItem.icon == newItem.icon
            }
            else -> false
        }
    }
}

class SearchLocationViewHolder(
    private val binding: ItemSearchLocationResultBinding,
    private val onItemClick: ((position: Int, latitude: Double, longitude: Double, placeName: String) -> Unit)?
) : RecyclerView.ViewHolder(binding.root) {

    companion object {
        const val itemViewType = 1
        fun create(
            parent: ViewGroup,
            onItemClick: ((position: Int, latitude: Double, longitude: Double, placeName: String) -> Unit)?
        ): SearchLocationViewHolder {
            return SearchLocationViewHolder(
                ItemSearchLocationResultBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false
                ),
                onItemClick
            )
        }
    }

    fun bind(item: SearchResult) {
        binding.placeNameTextView.text = item.placeName
        val locationText =
            "${item.latitude.latitudeCoordinates(itemView.context)}, ${item.longitude.longitudeCoordinates(
                itemView.context
            )}"
        binding.placeLocationTextView.text = locationText

        itemView.setOnClickListener {
            onItemClick?.invoke(adapterPosition, item.latitude, item.longitude, item.placeName)
        }
    }
}

class SearchLocationErrorViewHolder(private val binding: ItemSearchLocationResultErrorBinding) : RecyclerView.ViewHolder(binding.root) {

    companion object {
        const val itemViewType = 2
        fun create(parent: ViewGroup): SearchLocationErrorViewHolder {
            return SearchLocationErrorViewHolder(
                ItemSearchLocationResultErrorBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false
                )
            )
        }
    }

    fun bind(error: SearchResultError) {
        if (error.icon != 0) {
            binding.searchErrorIconImageView.setImageResource(error.icon)
        }
        binding.searchErrorTitleTextView.text = error.title
        binding.searchErrorMessageTextView.text = error.message
    }
}

data class SearchResult(
    val id: String?,
    val placeName: String,
    val latitude: Double,
    val longitude: Double
) :
    BaseListItem {
    override fun getItemId(): Int = 1
}

data class SearchResultError(
    val title: String,
    val message: String,
    @DrawableRes val icon: Int
) :
    BaseListItem {
    override fun getItemId(): Int = 2
}
