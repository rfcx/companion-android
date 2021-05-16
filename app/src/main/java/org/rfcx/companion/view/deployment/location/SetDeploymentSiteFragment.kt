package org.rfcx.companion.view.deployment.location

import android.content.Context
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.*
import androidx.fragment.app.Fragment
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.Transformations
import androidx.recyclerview.widget.LinearLayoutManager
import io.realm.Realm
import kotlinx.android.synthetic.main.fragment_set_deployment_site.*
import kotlinx.android.synthetic.main.layout_search_view.*
import org.rfcx.companion.R
import org.rfcx.companion.entity.EdgeDeployment
import org.rfcx.companion.entity.Locate
import org.rfcx.companion.entity.guardian.GuardianDeployment
import org.rfcx.companion.localdb.EdgeDeploymentDb
import org.rfcx.companion.localdb.LocateDb
import org.rfcx.companion.localdb.ProjectDb
import org.rfcx.companion.localdb.guardian.GuardianDeploymentDb
import org.rfcx.companion.util.*
import org.rfcx.companion.view.deployment.BaseDeploymentProtocol
import org.rfcx.companion.view.deployment.locate.SiteWithLastDeploymentItem
import org.rfcx.companion.view.map.SiteAdapter

class SetDeploymentSiteFragment : Fragment(),
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
    private val projectDb by lazy { ProjectDb(realm) }

    private val preferences by lazy { Preferences.getInstance(requireContext()) }

    // Local LiveData
    private lateinit var audioMothDeployLiveData: LiveData<List<EdgeDeployment>>
    private var audioMothDeployments = listOf<EdgeDeployment>()
    private val audioMothDeploymentObserve = Observer<List<EdgeDeployment>> {
        it.forEach { st ->
            Log.d("SiteJAA", st.stream!!.project!!.name)
        }
        this.audioMothDeployments = it.filter { deployment -> deployment.isCompleted() }
    }

    private lateinit var guardianDeploymentLiveData: LiveData<List<GuardianDeployment>>
    private var guardianDeployments = listOf<GuardianDeployment>()
    private val guardianDeploymentObserve = Observer<List<GuardianDeployment>> {
        it.forEach { st ->
            Log.d("SiteJAA", st.stream!!.project!!.name)
        }
        this.guardianDeployments = it.filter { deployment -> deployment.isCompleted() }
    }

    private lateinit var siteLiveData: LiveData<List<Locate>>
    private var sites = listOf<Locate>()
    private val siteObserve = Observer<List<Locate>> {
        this.sites = it
        it.forEach { st ->
            Log.d("SiteJAA", st.name)
        }
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
        initIntent()
    }

    private fun initIntent() {
        arguments?.let {
            latitude = it.getDouble(ARG_LATITUDE)
            longitude = it.getDouble(ARG_LONGITUDE)
        }
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
                    newList.addAll(sitesAdapter.filter { it.locate.name.toLowerCase().contains(text) })
                    noResultFound.visibility = View.GONE
                    val createNew = arrayListOf(
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
        val projectId = preferences.getInt(Preferences.SELECTED_PROJECT)
        val project = projectDb.getProjectById(projectId)
        val projectName = project?.name ?: getString(R.string.none)
        Log.d("Project Name", projectName)
        siteLiveData = Transformations.map(locateDb.getAllResultsAsyncWithinProject(project = projectName).asLiveData()) {
            it
        }
        siteLiveData.observeForever(siteObserve)

        audioMothDeployLiveData =
            Transformations.map(edgeDeploymentDb.getAllResultsAsyncWithinProject(project = projectName).asLiveData()) {
                it
            }
        audioMothDeployLiveData.observeForever(audioMothDeploymentObserve)

        guardianDeploymentLiveData =
            Transformations.map(guardianDeploymentDb.getAllResultsAsyncWithinProject(project = projectName).asLiveData()) {
                it
            }
        guardianDeploymentLiveData.observeForever(guardianDeploymentObserve)
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
