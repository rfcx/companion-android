package org.rfcx.audiomoth.view.map

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.Transformations
import androidx.viewpager2.widget.MarginPageTransformer
import androidx.viewpager2.widget.ViewPager2
import io.realm.Realm
import kotlinx.android.synthetic.main.fragment_deployment_view_pager.*
import org.rfcx.audiomoth.MainActivity
import org.rfcx.audiomoth.R
import org.rfcx.audiomoth.entity.Deployment
import org.rfcx.audiomoth.localdb.DeploymentDb
import org.rfcx.audiomoth.util.RealmHelper
import org.rfcx.audiomoth.util.asLiveData

class DeploymentViewPagerFragment : Fragment() {
    private val deploymentDb = DeploymentDb(Realm.getInstance(RealmHelper.migrationConfig()))
    private lateinit var deployLiveData: LiveData<List<Deployment>>
    private var deployments = listOf<Deployment>()

    private val deploymentObserve = Observer<List<Deployment>> {
        Log.d("features", "deploymentObserve ${it.size}")
        this.deployments = it
    }

    private var id: Int? = null
    private lateinit var viewPagerAdapter: DeploymentViewPagerAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initIntent()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_deployment_view_pager, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.d("features", "DeploymentViewPagerFragment $id")

        fetchData()
        initAdapter()

        id?.let {

            viewPagerAdapter.deployments = listOf(Deployment(), Deployment())
            Log.d("features", "deployments.size ${deployments.size}")

            val deploymentIndex = deployments.indexOf(deployments.find { it.id == this.id })
            Log.d("features", "deploymentIndex $deploymentIndex")

            deploymentViewPager.post {
                deploymentViewPager.setCurrentItem(deploymentIndex, false)
            }
        }
    }

    private fun fetchData() {
        deployLiveData = Transformations.map(deploymentDb.getAllResultsAsync().asLiveData()) {
            it
        }
        deployLiveData.observeForever(deploymentObserve)
    }

    private fun initIntent() {
        arguments?.let {
            id = it.getInt(ARG_ID)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        deployLiveData.removeObserver(deploymentObserve)
    }

    private fun initAdapter() {
        deploymentViewPager.clipToPadding = false
        deploymentViewPager.clipChildren = false
        deploymentViewPager.setPadding(
            resources.getDimensionPixelSize(R.dimen.viewpager_padding),
            0, resources.getDimensionPixelSize(R.dimen.viewpager_padding), 0
        )

        val marginTransformer =
            MarginPageTransformer(resources.getDimensionPixelSize(R.dimen.margin_padding_normal))
        deploymentViewPager.setPageTransformer(marginTransformer)
        deploymentViewPager.offscreenPageLimit = 2
        viewPagerAdapter = DeploymentViewPagerAdapter(childFragmentManager, lifecycle)
        deploymentViewPager.adapter = viewPagerAdapter
        deploymentViewPager.registerOnPageChangeCallback(object :
            ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                if (activity is MainActivity && position < viewPagerAdapter.itemCount) {
                    (activity as MainActivity).moveMapIntoReportMarker(
                        viewPagerAdapter.deployments[position]
                    )
                }
            }
        })
    }

    companion object {
        private const val ARG_ID = "ARG_ID"

        @JvmStatic
        fun newInstance(id: Int) =
            DeploymentViewPagerFragment().apply {
                arguments = Bundle().apply {
                    putInt(ARG_ID, id)
                }
            }
    }
}
