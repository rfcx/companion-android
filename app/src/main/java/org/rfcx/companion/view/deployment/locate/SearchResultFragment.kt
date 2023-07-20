package org.rfcx.companion.view.deployment.locate

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.android.synthetic.main.layout_search_result.*
import org.rfcx.companion.R
import org.rfcx.companion.entity.Screen
import org.rfcx.companion.util.Analytics

class SearchResultFragment : Fragment() {
    private val analytics by lazy { context?.let { Analytics(it) } }
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
    override fun onResume() {
        super.onResume()
        analytics?.trackScreen(Screen.MAP_SEARCH_RESULT)
    }
}
