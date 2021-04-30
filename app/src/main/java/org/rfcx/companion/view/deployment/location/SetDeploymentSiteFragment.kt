package org.rfcx.companion.view.deployment.location

import android.content.Context
import android.os.Bundle
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
import org.rfcx.companion.util.*
import org.rfcx.companion.view.deployment.BaseDeploymentProtocol
import org.rfcx.companion.view.deployment.locate.SiteWithLastDeploymentItem
import org.rfcx.companion.view.map.SiteAdapter

class SetDeploymentSiteFragment : Fragment(), SearchView.OnQueryTextListener, (Locate) -> Unit {

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
    private lateinit var siteLiveData: LiveData<List<Locate>>
    private var sites = listOf<Locate>()
    private val siteObserve = Observer<List<Locate>> {
        this.sites = it
        combinedData()
    }

    private lateinit var audioMothDeployLiveData: LiveData<List<EdgeDeployment>>
    private var audioMothDeployments = listOf<EdgeDeployment>()
    private val audioMothDeploymentObserve = Observer<List<EdgeDeployment>> {
        this.audioMothDeployments = it
        combinedData()
    }

    private lateinit var guardianDeploymentLiveData: LiveData<List<GuardianDeployment>>
    private var guardianDeployments = listOf<GuardianDeployment>()
    private val guardianDeploymentObserve = Observer<List<GuardianDeployment>> {
        this.guardianDeployments = it
        combinedData()
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        deploymentProtocol = context as BaseDeploymentProtocol
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.search_menu, menu)
        val searchItem = menu.findItem(R.id.searchView)
        val searchView: SearchView = searchItem.actionView as SearchView
        searchView.queryHint = getString(R.string.site_name_hint)
        searchView.setOnQueryTextListener(this)
        context?.let {
            DrawableCompat.setTint(
                DrawableCompat.wrap(searchItem.icon),
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
        setLiveData()
        setupTopBar()
        setupAdapter()
    }

    private fun combinedData() {
        existedSiteAdapter.items = arrayListOf()
        val lasLocation = context?.getLastLocation()
        if (lasLocation != null) {
            sitesAdapter = getListSite(
                requireContext(),
                audioMothDeployments.filter { it.isCompleted() },
                guardianDeployments.filter { it.isCompleted() },
                getString(R.string.none),
                lasLocation,
                sites
            )
        } else {
            sitesAdapter = getListSiteWithOutCurrentLocation(
                requireContext(),
                audioMothDeployments.filter { it.isCompleted() },
                guardianDeployments.filter { it.isCompleted() },
                getString(R.string.none),
                sites
            )
        }
        existedSiteAdapter.items = sitesAdapter
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
    override fun invoke(site: Locate) {
        TODO("Not yet implemented")
    }

    override fun onDestroy() {
        super.onDestroy()
        siteLiveData.removeObserver(siteObserve)
        audioMothDeployLiveData.removeObserver(audioMothDeploymentObserve)
        guardianDeploymentLiveData.removeObserver(guardianDeploymentObserve)
    }

    companion object {
        fun newInstance() = SetDeploymentSiteFragment()
    }
}
