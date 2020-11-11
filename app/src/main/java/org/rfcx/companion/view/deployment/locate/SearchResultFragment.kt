package org.rfcx.companion.view.deployment.locate

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.mapbox.api.geocoding.v5.MapboxGeocoding
import com.mapbox.api.geocoding.v5.models.CarmenFeature
import com.mapbox.api.geocoding.v5.models.GeocodingResponse
import com.mapbox.mapboxsdk.geometry.LatLng
import java.util.*
import kotlinx.android.synthetic.main.layout_search_result.*
import org.rfcx.companion.R
import org.rfcx.companion.adapter.BaseListItem
import org.rfcx.companion.entity.Screen
import org.rfcx.companion.util.Analytics
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class SearchResultFragment : Fragment() {
    private val analytics by lazy { context?.let { Analytics(it) } }
    private var mapBoxGeoCoding: MapboxGeocoding? = null

    private val searchLocationResultAdapter =
        SearchLocationResultAdapter()

    private var searchQuery: String? = null

    private val latLngRegex =
        Regex("^[-+]?([1-8]?\\d(\\.\\d+)?|90(\\.0+)?),\\s*[-+]?(180(\\.0+)?|((1[0-7]\\d)|([1-9]?\\d))(\\.\\d+)?)\$")

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.layout_search_result, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupAdapter()
        val initialSearchQuery = arguments?.getString(searchBundleKey)
        search(initialSearchQuery)
    }

    fun search(query: String?) {
        searchQuery = query
        if (query.isNullOrEmpty()) {
            showInitialSearchItem()
            return
        }
        if (searchLocationResultAdapter.itemCount > 0 &&
            searchLocationResultAdapter.getItemViewType(0) == SearchLocationErrorViewHolder.itemViewType
        ) {
            searchLocationResultAdapter.submitList(listOf())
        }

        mapBoxGeoCoding?.cancelCall()
        mapBoxGeoCoding = MapboxGeocoding.builder()
            .accessToken(getString(R.string.mapbox_token))
            .query(query)
            .limit(10)
            .build()

        mapBoxGeoCoding?.enqueueCall(object : Callback<GeocodingResponse> {
            override fun onFailure(call: Call<GeocodingResponse>, t: Throwable) {
                if (!call.isCanceled) {
                    searchLocationResultAdapter.showError(
                        SearchResultError(
                            getString(R.string.search_error_title),
                            getString(R.string.search_error_message),
                            R.drawable.ic_baseline_error_outline_24
                        )
                    )
                }
                t.printStackTrace()
            }

            override fun onResponse(
                call: Call<GeocodingResponse>,
                response: Response<GeocodingResponse>
            ) {
                if (response.isSuccessful && response.body() != null) {
                    val result = response.body()?.features()
                    showSearchResult(result)
                    analytics?.trackSearchLocationEvent()
                } else {
                    showSearchResult(null)
                }
            }
        })
    }

    private fun showSearchResult(result: List<CarmenFeature>?) {
        if (!isAdded || isDetached) return
        if (searchQuery.isNullOrEmpty()) {
            showInitialSearchItem()
            return
        }

        var locationFromSearchQuery: SearchResult? = null
        searchQuery?.let {
            val isLatLngPattern = latLngRegex.matches(it)
            if (isLatLngPattern) {
                val latLng = it.split(",").toTypedArray()
                if (latLng.count() == 2) {
                    try {
                        val latitude: Double = latLng[0].toDouble()
                        val longitude: Double = latLng[1].toDouble()
                        locationFromSearchQuery =
                            SearchResult(it, it, latitude, longitude)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        }

        if (result.isNullOrEmpty() && locationFromSearchQuery == null) {
            searchLocationResultAdapter.showError(
                SearchResultError(
                    getString(R.string.search_not_found_title),
                    getString(R.string.search_not_found_message),
                    R.drawable.ic_baseline_error_outline_24
                )
            )
        } else {
            val list: List<SearchResult> = result?.mapNotNull { carmenFeature ->
                if (carmenFeature.center() != null && carmenFeature.placeName() != null) {
                    SearchResult(
                        id = carmenFeature.id(),
                        placeName = carmenFeature.placeName()!!,
                        latitude = carmenFeature.center()!!.latitude(),
                        longitude = carmenFeature.center()!!.longitude()
                    )
                } else {
                    null
                }
            } ?: listOf()
            val arr = ArrayList<BaseListItem>(list)
            // Add the last item with query location
            locationFromSearchQuery?.let {
                arr.add(it)
            }
            searchLocationResultAdapter.submitList(arr)
        }
    }

    private fun showInitialSearchItem() {
        searchLocationResultAdapter.showError(
            SearchResultError(
                getString(R.string.search_title),
                getString(R.string.search_message),
                R.drawable.ic_baseline_search_24
            )
        )
    }

    private fun setupAdapter() {
        searchLocationResultAdapter.onItemClick = { _, latitude, longitude, placeName ->
            (parentFragment as OnSearchResultListener?)?.onLocationSelected(
                LatLng(latitude, longitude), placeName
            )
        }
        searchLocationRecyclerView.apply {
            setHasFixedSize(false)
            layoutManager = LinearLayoutManager(context)
            adapter = searchLocationResultAdapter
        }
    }

    companion object {
        const val tag = "SearchResultFragment"
        const val searchBundleKey = "SearchResultFragment::searchBundleKey"
        fun newInstance(query: String?) = SearchResultFragment().apply {
            arguments = Bundle().apply {
                putString(searchBundleKey, query)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        mapBoxGeoCoding?.cancelCall()
    }

    override fun onResume() {
        super.onResume()
        analytics?.trackScreen(Screen.MAP_SEARCH_RESULT)
    }

    interface OnSearchResultListener {
        fun onLocationSelected(latLng: LatLng, placeName: String)
    }
}
