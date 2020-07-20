package org.rfcx.audiomoth.view.deployment.locate

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
import kotlinx.android.synthetic.main.layout_search_result.*
import org.rfcx.audiomoth.R
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class SearchResultFragment : Fragment() {

    private var mapBoxGeoCoding: MapboxGeocoding? = null

    private val searchLocationResultAdapter =
        SearchLocationResultAdapter()

    private var searchQuery: String? = null

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
                } else {
                    showSearchResult(null)
                }
            }
        })
    }

    private fun showSearchResult(result: List<CarmenFeature>?) {

        if (searchQuery.isNullOrEmpty()) {
            showInitialSearchItem()
            return
        }

        if (result.isNullOrEmpty()) {
            searchLocationResultAdapter.showError(
                SearchResultError(
                    getString(R.string.search_not_found_title),
                    getString(R.string.search_not_found_message),
                    R.drawable.ic_baseline_error_outline_24
                )
            )

        } else {
            val list: List<SearchResult> = result.mapNotNull { carmenFeature ->
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
            }
            searchLocationResultAdapter.submitList(list)
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

    interface OnSearchResultListener {
        fun onLocationSelected(latLng: LatLng, placeName: String)
    }
}