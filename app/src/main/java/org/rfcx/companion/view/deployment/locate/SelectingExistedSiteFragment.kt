package org.rfcx.companion.view.deployment.locate

import android.content.Context
import android.location.Location
import android.location.LocationManager
import android.os.Bundle
import android.view.*
import android.view.inputmethod.InputMethodManager
import android.widget.SearchView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.Transformations
import androidx.recyclerview.widget.LinearLayoutManager
import io.realm.Realm
import kotlinx.android.synthetic.main.fragment_selecting_existed_site.*
import org.rfcx.companion.R
import org.rfcx.companion.entity.EdgeDeployment
import org.rfcx.companion.entity.Locate
import org.rfcx.companion.entity.guardian.GuardianDeployment
import org.rfcx.companion.entity.response.ProjectResponse
import org.rfcx.companion.localdb.EdgeDeploymentDb
import org.rfcx.companion.localdb.LocateDb
import org.rfcx.companion.localdb.LocationGroupDb
import org.rfcx.companion.localdb.guardian.GuardianDeploymentDb
import org.rfcx.companion.repo.ApiManager
import org.rfcx.companion.service.DownloadStreamState
import org.rfcx.companion.util.*
import org.rfcx.companion.view.deployment.BaseDeploymentProtocol
import org.rfcx.companion.view.detail.MapPickerProtocol
import org.rfcx.companion.view.map.SiteAdapter
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

// TODO DELETE
class SelectingExistedSiteFragment : Fragment(), SearchView.OnQueryTextListener, (Locate, Boolean) -> Unit {
    private val existedSiteAdapter by lazy { SiteAdapter(this) }
    private var mapPickerProtocol: MapPickerProtocol? = null
    private var deploymentProtocol: BaseDeploymentProtocol? = null

    val realm: Realm = Realm.getInstance(RealmHelper.migrationConfig())
    private val locateDb by lazy { LocateDb(realm) }
    private val locationGroupDb by lazy { LocationGroupDb(realm) }
    private val edgeDeploymentDb by lazy { EdgeDeploymentDb(realm) }
    private val guardianDeploymentDb by lazy { GuardianDeploymentDb(realm) }

    private var latitude: Double = 0.0
    private var longitude: Double = 0.0
    private var lastLocation: Location? = null

    private lateinit var locateLiveData: LiveData<List<Locate>>
    private var locations = listOf<Locate>()

    private lateinit var edgeDeployLiveData: LiveData<List<EdgeDeployment>>
    private var edgeDeployments = listOf<EdgeDeployment>()
    private lateinit var guardianDeploymentLiveData: LiveData<List<GuardianDeployment>>
    private var guardianDeployments = listOf<GuardianDeployment>()

    private var sites = arrayListOf<SiteWithLastDeploymentItem>()

    private val locateObserve = Observer<List<Locate>> {
        this.locations = it
        setupView()
        if (deploymentProtocol?.isSiteLoading() == DownloadStreamState.FINISH) {
            showDialog()
        }
    }

    private val edgeDeploymentObserve = Observer<List<EdgeDeployment>> {
        this.edgeDeployments = it
        setupView()
    }

    private val guardianDeploymentObserve = Observer<List<GuardianDeployment>> {
        this.guardianDeployments = it
        setupView()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initIntent()
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

    override fun onAttach(context: Context) {
        super.onAttach(context)
        mapPickerProtocol = context as MapPickerProtocol
        deploymentProtocol = context as BaseDeploymentProtocol
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
        return inflater.inflate(R.layout.fragment_selecting_existed_site, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        existedSiteRecyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = existedSiteAdapter
        }

        val lastLocate = Location(LocationManager.GPS_PROVIDER)
        lastLocate.latitude = latitude
        lastLocate.longitude = longitude
        lastLocation = lastLocate

        getSiteData()
        setupView()

        showDialog()
    }

    private fun showDialog() {
        when (deploymentProtocol?.isSiteLoading()) {
            DownloadStreamState.RUNNING -> deploymentProtocol?.showSiteLoadingDialog(
                requireContext().getString(
                    R.string.sites_loading_dialog
                )
            )
            DownloadStreamState.FINISH -> deploymentProtocol?.showSiteLoadingDialog(
                requireContext().getString(
                    R.string.sites_loading_finish_dialog
                )
            )
        }
    }

    private fun getSiteData() {
        locateLiveData = Transformations.map(locateDb.getAllResultsAsync().asLiveData()) {
            it
        }
        locateLiveData.observeForever(locateObserve)

        edgeDeployLiveData =
            Transformations.map(edgeDeploymentDb.getAllResultsAsync().asLiveData()) {
                it
            }
        edgeDeployLiveData.observeForever(edgeDeploymentObserve)

        guardianDeploymentLiveData =
            Transformations.map(guardianDeploymentDb.getAllResultsAsync().asLiveData()) {
            it
        }
        guardianDeploymentLiveData.observeForever(guardianDeploymentObserve)
    }

    private fun setupView() {
        val currentLocation = lastLocation
        if (currentLocation != null) {
            sites = getListSite(
                requireContext(),
                edgeDeployments.filter { it.isCompleted() },
                guardianDeployments.filter { it.isCompleted() },
                "None",
                currentLocation,
                locations
            )
            existedSiteAdapter.items = ArrayList(createNewItem() + sites)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        locateLiveData.removeObserver(locateObserve)
        edgeDeployLiveData.removeObserver(edgeDeploymentObserve)
        guardianDeploymentLiveData.removeObserver(guardianDeploymentObserve)
    }

    companion object {
        const val tag = "SelectingExistedSiteFragment"
        const val ARG_LATITUDE = "ARG_LATITUDE"
        const val ARG_LONGITUDE = "ARG_LONGITUDE"

        @JvmStatic
        fun newInstance(latitude: Double, longitude: Double) = SelectingExistedSiteFragment()
            .apply {
                arguments = Bundle().apply {
                    putDouble(ARG_LATITUDE, latitude)
                    putDouble(ARG_LONGITUDE, longitude)
                }
            }
    }

    private fun retrieveProjects(context: Context) {
        val token = "Bearer ${context.getIdToken()}"
        ApiManager.getInstance().getDeviceApi().getProjects(token)
            .enqueue(object : Callback<List<ProjectResponse>> {
                override fun onFailure(call: Call<List<ProjectResponse>>, t: Throwable) {
                    if (context.isNetworkAvailable()) {
                        Toast.makeText(context, R.string.error_has_occurred, Toast.LENGTH_SHORT)
                            .show()
                    }
                }

                override fun onResponse(
                    call: Call<List<ProjectResponse>>,
                    response: Response<List<ProjectResponse>>
                ) {
                    response.body()?.forEach { item ->
                        locationGroupDb.insertOrUpdate(item)
                    }
                }
            })
    }

    override fun invoke(locate: Locate, isNew: Boolean) {
        view?.hideKeyboard()
        context?.let { retrieveProjects(it) }
    }

    override fun onQueryTextSubmit(query: String?): Boolean {
        return true
    }

    override fun onQueryTextChange(newText: String?): Boolean {
        if (newText != null) {
            val text = newText.toLowerCase()
            val newList: ArrayList<SiteWithLastDeploymentItem> = ArrayList()
            newList.addAll(sites.filter { it.locate.name.toLowerCase().contains(text) })
            noResultFound.visibility = if (newList.isEmpty()) View.VISIBLE else View.GONE
            existedSiteAdapter.setFilter(ArrayList(createNewItem() + newList))
        }
        return true
    }

    private fun View.hideKeyboard() = this.let {
        val inputManager =
            context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        inputManager.hideSoftInputFromWindow(windowToken, 0)
    }

    private fun createNewItem(): List<SiteWithLastDeploymentItem> {
        return listOf(
            SiteWithLastDeploymentItem(
                Locate(
                    id = -1,
                    name = getString(R.string.create_new_site),
                    latitude = latitude,
                    longitude = longitude
                ),
                null,
                0F
            )
        )
    }
}
