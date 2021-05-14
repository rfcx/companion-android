package org.rfcx.companion.view.deployment.location

import android.content.Context
import android.graphics.Rect
import android.location.Location
import android.location.LocationManager
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.*
import android.widget.SearchView
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.Transformations
import androidx.recyclerview.widget.LinearLayoutManager
import io.realm.Realm
import kotlinx.android.synthetic.main.fragment_set_deployment_site.*
import org.rfcx.companion.R
import org.rfcx.companion.entity.EdgeDeployment
import org.rfcx.companion.entity.Locate
import org.rfcx.companion.entity.guardian.GuardianDeployment
import org.rfcx.companion.localdb.EdgeDeploymentDb
import org.rfcx.companion.localdb.LocateDb
import org.rfcx.companion.localdb.guardian.GuardianDeploymentDb
import org.rfcx.companion.util.RealmHelper
import org.rfcx.companion.util.asLiveData
import org.rfcx.companion.util.getListSite
import org.rfcx.companion.util.showKeyboard
import org.rfcx.companion.view.deployment.BaseDeploymentProtocol
import org.rfcx.companion.view.deployment.locate.SiteWithLastDeploymentItem
import org.rfcx.companion.view.map.SiteAdapter

class SetDeploymentSiteFragment : Fragment(), SearchView.OnQueryTextListener,
        (Locate, Boolean) -> Unit {

    // Protocol
    private var deploymentProtocol: BaseDeploymentProtocol? = null

    // Adapter
    private val existedSiteAdapter by lazy { SiteAdapter(this) }
    private var sitesAdapter = arrayListOf<SiteWithLastDeploymentItem>()

    // Local database
    val realm: Realm = Realm.getInstance(RealmHelper.migrationConfig())
    private val locateDb by lazy { LocateDb(realm) }
    private val edgeDeploymentDb by lazy { EdgeDeploymentDb(realm) }
    private val guardianDeploymentDb by lazy { GuardianDeploymentDb(realm) }

    // Local LiveData
    private lateinit var audioMothDeployLiveData: LiveData<List<EdgeDeployment>>
    private var audioMothDeployments = listOf<EdgeDeployment>()
    private val audioMothDeploymentObserve = Observer<List<EdgeDeployment>> {
        this.audioMothDeployments = it.filter { deployment -> deployment.isCompleted() }
    }

    private lateinit var guardianDeploymentLiveData: LiveData<List<GuardianDeployment>>
    private var guardianDeployments = listOf<GuardianDeployment>()
    private val guardianDeploymentObserve = Observer<List<GuardianDeployment>> {
        this.guardianDeployments = it.filter { deployment -> deployment.isCompleted() }
    }

    private lateinit var siteLiveData: LiveData<List<Locate>>
    private var sites = listOf<Locate>()
    private val siteObserve = Observer<List<Locate>> {
        this.sites = it
        setupView()
    }

    private var searchItem: MenuItem? = null
    private var latitude: Double = 0.0
    private var longitude: Double = 0.0

    override fun onAttach(context: Context) {
        super.onAttach(context)
        deploymentProtocol = context as BaseDeploymentProtocol
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
        initIntent()
    }

    private fun initIntent() {
        arguments?.let {
            latitude = it.getDouble(ARG_LATITUDE)
            longitude = it.getDouble(ARG_LONGITUDE)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.search_menu, menu)
        searchItem = menu.findItem(R.id.searchView)
        val menuItem = searchItem
        val searchView: SearchView = menuItem?.actionView as SearchView
        searchView.queryHint = getString(R.string.site_name_hint)
        searchView.setOnQueryTextListener(this)
        context?.let {
            DrawableCompat.setTint(
                DrawableCompat.wrap(menuItem.icon),
                ContextCompat.getColor(it, R.color.iconColor)
            )
        }
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_set_deployment_site, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupAdapter()
        setupTopBar()
        setLiveData()
        setEditText()
        siteNameEditText.showKeyboard()

        view.viewTreeObserver.addOnGlobalLayoutListener { setOnFocusEditText() }

        searchItem?.setOnMenuItemClickListener {
            siteNameEditText.clearFocus()
            true
        }
    }

    private fun setOnFocusEditText() {
        val screenHeight: Int = view?.rootView?.height ?: 0
        val r = Rect()
        view?.getWindowVisibleDisplayFrame(r)
        val keypadHeight: Int = screenHeight - r.bottom
        if (keypadHeight > screenHeight * 0.15) {
            val searchItem = searchItem?.actionView as SearchView
            if (searchItem.isIconified) siteNameEditText.requestFocus()
        } else {
            if (siteNameEditText != null) siteNameEditText.clearFocus()
        }
    }

    private fun setEditText() {
        siteNameEditText.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                searchItem?.isVisible = s?.length == 0
                if (s?.length == 0) {
                    existedSiteAdapter.isNewSite = false
                    existedSiteAdapter.items = sitesAdapter
                } else {
                    existedSiteAdapter.isNewSite = true
                    existedSiteAdapter.items = arrayListOf(
                        SiteWithLastDeploymentItem(
                            Locate(
                                id = -1,
                                name = s.toString(),
                                latitude = 0.0,
                                longitude = 0.0
                            ),
                            null,
                            0F
                        )
                    )
                }
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })
    }

    private fun setupView() {
        existedSiteAdapter.items = arrayListOf()
        val items = deploymentProtocol?.getSiteItem() ?: arrayListOf()
        val loc = Location(LocationManager.GPS_PROVIDER)
        loc.latitude = latitude
        loc.longitude = longitude
        sitesAdapter = getListSite(
            requireContext(),
            audioMothDeployments,
            guardianDeployments,
            getString(R.string.none),
            loc,
            sites
        )
        if (sitesAdapter[0].date == null && items.isNotEmpty()){
            handleItemsAdapter(items)
        } else {
            handleItemsAdapter(sitesAdapter)
        }
    }

    private fun handleItemsAdapter(sites: ArrayList<SiteWithLastDeploymentItem>) {
        existedSiteAdapter.isNewSite = false
        existedSiteAdapter.items = sites
        deploymentProtocol?.setSiteItem(sites)
    }

    private fun setupTopBar() {
        deploymentProtocol?.let {
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

    private fun setLiveData() {
        siteLiveData = Transformations.map(locateDb.getAllResultsAsync().asLiveData()) {
            it
        }
        siteLiveData.observeForever(siteObserve)

        audioMothDeployLiveData =
            Transformations.map(edgeDeploymentDb.getAllResultsAsync().asLiveData()) {
                it
            }
        audioMothDeployLiveData.observeForever(audioMothDeploymentObserve)

        guardianDeploymentLiveData =
            Transformations.map(guardianDeploymentDb.getAllResultsAsync().asLiveData()) {
                it
            }
        guardianDeploymentLiveData.observeForever(guardianDeploymentObserve)
    }

    override fun onQueryTextSubmit(query: String?): Boolean {
        return true
    }

    override fun onQueryTextChange(newText: String?): Boolean {
        if (newText != null) {
            val text = newText.toLowerCase()
            val newList: ArrayList<SiteWithLastDeploymentItem> = ArrayList()
            newList.addAll(sitesAdapter.filter { it.locate.name.toLowerCase().contains(text) })
            noResultFound.visibility = if (newList.isEmpty()) View.VISIBLE else View.GONE
            existedSiteAdapter.setFilter(ArrayList(newList))
        }
        return true
    }

    // On click site item
    override fun invoke(site: Locate, isNewSite: Boolean) {
        deploymentProtocol?.startDetailDeploymentSite(site.id, site.name, isNewSite)
    }

    override fun onDestroy() {
        super.onDestroy()
        siteLiveData.removeObserver(siteObserve)
        audioMothDeployLiveData.removeObserver(audioMothDeploymentObserve)
        guardianDeploymentLiveData.removeObserver(guardianDeploymentObserve)
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
