package org.rfcx.audiomoth.view.deployment.locate

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.DrawableRes
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.item_search_location_result.view.*
import kotlinx.android.synthetic.main.item_search_location_result_error.view.*
import org.rfcx.audiomoth.R
import org.rfcx.audiomoth.adapter.BaseListItem
import org.rfcx.audiomoth.util.latitudeCoordinates
import org.rfcx.audiomoth.util.longitudeCoordinates

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
    itemView: View,
    private val onItemClick: ((position: Int, latitude: Double, longitude: Double, placeName: String) -> Unit)?
) : RecyclerView.ViewHolder(itemView) {

    companion object {
        const val itemViewType = 1
        fun create(
            parent: ViewGroup,
            onItemClick: ((position: Int, latitude: Double, longitude: Double, placeName: String) -> Unit)?
        ): SearchLocationViewHolder {
            return SearchLocationViewHolder(
                LayoutInflater.from(parent.context).inflate(
                    R.layout.item_search_location_result,
                    parent,
                    false
                ), onItemClick
            )
        }
    }

    fun bind(item: SearchResult) {
        itemView.placeNameTextView.text = item.placeName
        val locationText =
            "${item.latitude.latitudeCoordinates(itemView.context)}, ${item.longitude.longitudeCoordinates(
                itemView.context
            )}"
        itemView.placeLocationTextView.text = locationText

        itemView.setOnClickListener {
            onItemClick?.invoke(adapterPosition, item.latitude, item.longitude, item.placeName)
        }
    }
}

class SearchLocationErrorViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

    companion object {
        const val itemViewType = 2
        fun create(parent: ViewGroup): SearchLocationErrorViewHolder {
            return SearchLocationErrorViewHolder(
                LayoutInflater.from(parent.context).inflate(
                    R.layout.item_search_location_result_error,
                    parent,
                    false
                )
            )
        }
    }

    fun bind(error: SearchResultError) {
        if (error.icon != 0) {
            itemView.searchErrorIconImageView.setImageResource(error.icon)
        }
        itemView.searchErrorTitleTextView.text = error.title
        itemView.searchErrorMessageTextView.text = error.message
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