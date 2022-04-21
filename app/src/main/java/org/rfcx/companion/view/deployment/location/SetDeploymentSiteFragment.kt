package org.rfcx.companion.view.deployment.location

import android.content.Context
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.android.synthetic.main.fragment_set_deployment_site.*
import kotlinx.android.synthetic.main.layout_search_view.*
import org.rfcx.companion.R
import org.rfcx.companion.base.ViewModelFactory
import org.rfcx.companion.entity.Stream
import org.rfcx.companion.repo.api.CoreApiHelper
import org.rfcx.companion.repo.api.CoreApiServiceImpl
import org.rfcx.companion.repo.api.DeviceApiHelper
import org.rfcx.companion.repo.api.DeviceApiServiceImpl
import org.rfcx.companion.repo.local.LocalDataHelper
import org.rfcx.companion.service.DownloadStreamsWorker
import org.rfcx.companion.util.*
import org.rfcx.companion.view.deployment.AudioMothDeploymentViewModel
import org.rfcx.companion.view.deployment.BaseDeploymentProtocol
import org.rfcx.companion.view.deployment.locate.SiteWithLastDeploymentItem
import org.rfcx.companion.view.map.SiteAdapter
import org.rfcx.companion.view.map.SyncInfo

class SetDeploymentSiteFragment :
    Fragment(),
    (Stream, Boolean) -> Unit {
    private lateinit var audioMothDeploymentViewModel: AudioMothDeploymentViewModel

    // Protocol
    private var deploymentProtocol: BaseDeploymentProtocol? = null

    // Adapter
    private val existedSiteAdapter by lazy { SiteAdapter(this) }
    private var sitesAdapter = arrayListOf<SiteWithLastDeploymentItem>()

    private val preferences by lazy { Preferences.getInstance(requireContext()) }

    private var lastSyncingInfo: SyncInfo? = null
    private var searchItem: MenuItem? = null
    private var latitude: Double = 0.0
    private var longitude: Double = 0.0

    override fun onAttach(context: Context) {
        super.onAttach(context)
        deploymentProtocol = context as BaseDeploymentProtocol
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initIntent()
    }

    private fun setViewModel() {
        audioMothDeploymentViewModel = ViewModelProvider(
            this,
            ViewModelFactory(
                requireActivity().application,
                DeviceApiHelper(DeviceApiServiceImpl()),
                CoreApiHelper(CoreApiServiceImpl()),
                LocalDataHelper()
            )
        ).get(AudioMothDeploymentViewModel::class.java)
    }

    private fun initIntent() {
        arguments?.let {
            latitude = it.getDouble(ARG_LATITUDE)
            longitude = it.getDouble(ARG_LONGITUDE)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_set_deployment_site, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setViewModel()
        setObserver()
        setupAdapter()
        setupTopBar()
        setEditText()
        setSwipeSite()
    }

    private fun updateSyncInfo(syncInfo: SyncInfo? = null, isSites: Boolean) {
        val status = syncInfo
            ?: if (context.isNetworkAvailable()) SyncInfo.Starting else SyncInfo.WaitingNetwork
        if (this.lastSyncingInfo == SyncInfo.Uploaded && status == SyncInfo.Uploaded) return
        this.lastSyncingInfo = status
        when (status) {
            SyncInfo.Starting, SyncInfo.Uploading -> {
                statusSiteView.onShow(getString(R.string.sites_downloading))
            }
            SyncInfo.Uploaded -> {
                statusSiteView.onShowWithDelayed(getString(R.string.sites_synced))
            }
            else -> {
                statusSiteView.onShowWithDelayed(getString(R.string.format_deploy_waiting_network))
            }
        }
    }

    private fun setEditText() {
        searchLayoutSearchEditText.showKeyboard()
        searchLayoutSearchEditText.requestFocus()
        searchViewActionRightButton.visibility = View.VISIBLE
        searchLayoutSearchEditText.hint = context?.getString(R.string.search_or_create_box_hint)

        searchViewActionRightButton.setOnClickListener {
            if (searchLayoutSearchEditText.text.isNullOrBlank()) {
                it.hideKeyboard()
            } else {
                searchLayoutSearchEditText.text = null
            }
        }

        searchLayoutSearchEditText.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                searchItem?.isVisible = s?.length == 0
                if (s?.length == 0) {
                    existedSiteAdapter.isNewSite = false
                    existedSiteAdapter.items = sitesAdapter
                } else {
                    val text = s.toString().toLowerCase()
                    val newList: ArrayList<SiteWithLastDeploymentItem> = ArrayList()
                    newList.addAll(
                        sitesAdapter.filter {
                            it.stream.name.toLowerCase().contains(text)
                        }
                    )
                    noResultFound.visibility = View.GONE
                    val createNew = arrayListOf(
                        SiteWithLastDeploymentItem(
                            Stream(
                                id = -1,
                                name = s.toString(),
                                latitude = 0.0,
                                longitude = 0.0
                            ),
                            null,
                            0F
                        )
                    )
                    existedSiteAdapter.setFilter(ArrayList(createNew + newList))
                    existedSiteAdapter.isNewSite = true
                }
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })
    }

    private fun setupView() {
        existedSiteAdapter.items = arrayListOf()
        val items = deploymentProtocol?.getSiteItem() ?: arrayListOf()
        sitesAdapter = items
        handleItemsAdapter(items)
    }

    private fun handleItemsAdapter(sites: ArrayList<SiteWithLastDeploymentItem>) {
        existedSiteAdapter.isNewSite = false
        existedSiteAdapter.items = sites
        deploymentProtocol?.setSiteItem(sites)
    }

    private fun setupTopBar() {
        deploymentProtocol?.let {
            it.setToolbarSubtitle()
            it.setMenuToolbar(false)
            it.showToolbar()
            it.setToolbarTitle()
        }
    }

    private fun setupAdapter() {
        existedRecyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = existedSiteAdapter
        }
    }

    private fun setObserver() {
        audioMothDeploymentViewModel.getSites().observe(
            viewLifecycleOwner,
            Observer {
                setupView()
            }
        )

        audioMothDeploymentViewModel.downloadStreamsWork().observe(
            viewLifecycleOwner,
            Observer {
                when (it.status) {
                    Status.LOADING -> updateSyncInfo(SyncInfo.Uploading, true)
                    Status.SUCCESS -> updateSyncInfo(SyncInfo.Uploaded, true)
                    Status.ERROR -> updateSyncInfo(isSites = true)
                }
            }
        )
    }

    private fun setSwipeSite() {
        siteSwipeRefreshView.apply {
            setOnRefreshListener {
                val projectId = preferences.getInt(Preferences.SELECTED_PROJECT)
                val project = audioMothDeploymentViewModel.getProjectById(projectId)
                project?.serverId?.let {
                    DownloadStreamsWorker.enqueue(context, it)
                }
                isRefreshing = false
            }
            setColorSchemeResources(R.color.colorPrimary)
        }
    }

    // On click site item
    override fun invoke(site: Stream, isNewSite: Boolean) {
        deploymentProtocol?.startDetailDeploymentSite(site.id, isNewSite)
    }

    override fun onDestroy() {
        super.onDestroy()
        audioMothDeploymentViewModel.onDestroy()
    }

    companion object {
        const val ARG_LATITUDE = "ARG_LATITUDE"
        const val ARG_LONGITUDE = "ARG_LONGITUDE"

        fun newInstance(lat: Double, lng: Double) =
            SetDeploymentSiteFragment().apply {
                arguments = Bundle().apply {
                    putDouble(ARG_LATITUDE, lat)
                    putDouble(ARG_LONGITUDE, lng)
                }
            }
    }
}
