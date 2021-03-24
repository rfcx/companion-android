package org.rfcx.companion.view.deployment.locate

import android.content.Context
import android.location.Location
import android.location.LocationManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.Transformations
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.snackbar.Snackbar
import io.realm.Realm
import kotlinx.android.synthetic.main.fragment_selecting_existed_site.*
import org.rfcx.companion.R
import org.rfcx.companion.entity.Locate
import org.rfcx.companion.entity.response.ProjectResponse
import org.rfcx.companion.localdb.LocateDb
import org.rfcx.companion.localdb.LocationGroupDb
import org.rfcx.companion.repo.ApiManager
import org.rfcx.companion.service.DownloadStreamState
import org.rfcx.companion.service.DownloadStreamsWorker
import org.rfcx.companion.util.RealmHelper
import org.rfcx.companion.util.asLiveData
import org.rfcx.companion.util.getIdToken
import org.rfcx.companion.util.isNetworkAvailable
import org.rfcx.companion.view.deployment.EdgeDeploymentProtocol
import org.rfcx.companion.view.detail.MapPickerProtocol
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class SelectingExistedSiteFragment : Fragment(), (Locate) -> Unit {
    private val existedSiteAdapter by lazy { ExistedSiteAdapter(this) }
    private var mapPickerProtocol: MapPickerProtocol? = null
    private var deploymentProtocol: EdgeDeploymentProtocol? = null

    val realm: Realm = Realm.getInstance(RealmHelper.migrationConfig())
    private val locateDb by lazy { LocateDb(realm) }
    private val locationGroupDb by lazy { LocationGroupDb(realm) }
    private var latitude: Double = 0.0
    private var longitude: Double = 0.0
    private var lastLocation: Location? = null

    private lateinit var locateLiveData: LiveData<List<Locate>>
    private var locations = listOf<Locate>()

    private val locateObserve = Observer<List<Locate>> {
        this.locations = it
        setupView()
        if (deploymentProtocol?.isSiteLoading() == DownloadStreamState.FINISH) {
            showDialog()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initIntent()
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        mapPickerProtocol = context as MapPickerProtocol
        deploymentProtocol = context as EdgeDeploymentProtocol
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
        when(deploymentProtocol?.isSiteLoading()) {
            DownloadStreamState.RUNNING -> deploymentProtocol?.showSiteLoadingDialog(requireContext().getString(R.string.sites_loading_dialog))
            DownloadStreamState.FINISH -> deploymentProtocol?.showSiteLoadingDialog(requireContext().getString(R.string.sites_loading_finish_dialog))
        }
    }

    private fun getSiteData() {
        locateLiveData = Transformations.map(locateDb.getAllResultsAsync().asLiveData()) {
            it
        }
        locateLiveData.observeForever(locateObserve)
    }

    private fun setupView() {
        val nearLocations =
            findNearLocations(lastLocation, ArrayList(locations))?.sortedBy { it.second }
        val createNew = listOf(
            SiteItem(
                Locate(
                    id = -1,
                    name = getString(R.string.create_new_site),
                    latitude = latitude,
                    longitude = longitude
                ),
                0F
            )
        )
        val locationsItems: List<SiteItem> = nearLocations?.map { SiteItem(it.first, it.second) } ?: listOf()
        existedSiteAdapter.items = createNew + locationsItems
    }

    private fun findNearLocations(
        lastLocation: Location?,
        locateItems: ArrayList<Locate>
    ): List<Pair<Locate, Float>>? {
        lastLocation ?: return null

        if (locateItems.isNotEmpty()) {
            val itemsWithDistance = arrayListOf<Pair<Locate, Float>>()
            // Find locate distances
            locateItems.mapTo(itemsWithDistance, {
                val loc = Location(LocationManager.GPS_PROVIDER)
                loc.latitude = it.latitude
                loc.longitude = it.longitude
                val distance = loc.distanceTo(this.lastLocation) // return in meters
                Pair(it, distance)
            })
            return itemsWithDistance
        }
        return null
    }

    override fun onDestroy() {
        super.onDestroy()
        locateLiveData.removeObserver(locateObserve)
    }

    companion object {
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

    override fun invoke(locate: Locate) {
        mapPickerProtocol?.startLocationPage(
            locate.latitude,
            locate.longitude,
            locate.altitude,
            locate.name,
            false
        )
        context?.let { retrieveProjects(it) }
    }
}
