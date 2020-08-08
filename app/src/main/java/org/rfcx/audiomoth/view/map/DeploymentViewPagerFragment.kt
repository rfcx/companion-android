package org.rfcx.audiomoth.view.map

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.viewpager2.widget.MarginPageTransformer
import androidx.viewpager2.widget.ViewPager2
import io.realm.Realm
import kotlinx.android.synthetic.main.fragment_deployment_view_pager.*
import org.rfcx.audiomoth.DeploymentListener
import org.rfcx.audiomoth.MainActivity
import org.rfcx.audiomoth.R
import org.rfcx.audiomoth.entity.DeploymentState
import org.rfcx.audiomoth.localdb.guardian.GuardianDeploymentDb
import org.rfcx.audiomoth.util.RealmHelper
import org.rfcx.audiomoth.util.WifiHotspotUtils
import org.rfcx.audiomoth.view.deployment.EdgeDeploymentActivity
import org.rfcx.audiomoth.view.detail.DeploymentDetailActivity
import org.rfcx.audiomoth.view.diagnostic.DiagnosticActivity

class DeploymentViewPagerFragment : Fragment(), DeploymentDetailClickListener {
    private val guardianDeploymentDb by lazy {
        GuardianDeploymentDb(Realm.getInstance(RealmHelper.migrationConfig()))
    }
    private var deploymentListener: DeploymentListener? = null
    private lateinit var viewPagerAdapter: DeploymentViewPagerAdapter
    private var selectedId: Int? = null // selected deployment id
    private var currentPosition: Int = 0

    override fun onAttach(context: Context) {
        super.onAttach(context)
        deploymentListener = context as DeploymentListener
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initIntent()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_deployment_view_pager, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initAdapter()
        setViewPagerAdapter()
    }

    // Region {DeploymentViewPagerAdapter.DeploymentDetailClickListener}
    override fun onClickedEdgeDeploymentDetail(edgeDeploymentView: DeploymentDetailView.EdgeDeploymentView) {
        context?.let {
            val isReadyToUpload = edgeDeploymentView.state == DeploymentState.Edge.ReadyToUpload.key
            if (isReadyToUpload) {
                DeploymentDetailActivity.startActivity(it, edgeDeploymentView.id)
            } else {
                EdgeDeploymentActivity.startActivity(it, edgeDeploymentView.id)
            }
        }
    }

    override fun onClickedGuardianDeploymentDetail(guardianDeploymentView: DeploymentDetailView.GuardianDeploymentView) {
        val guardianDeployment = guardianDeploymentDb.getDeploymentById(guardianDeploymentView.id)
        if (context != null && guardianDeployment != null) {
            DiagnosticActivity.startActivity(
                requireContext(), guardianDeployment,
                WifiHotspotUtils.isConnectedWithGuardian(
                    requireContext(), guardianDeploymentView.wifiName ?: ""
                )
            )
        }
    }
    // Endregion

    private fun setViewPagerAdapter() {
        val showDeployments = deploymentListener?.getShowDeployments()
        if (showDeployments != null) {
            viewPagerAdapter.submitList(showDeployments) // adapter update items
            setSelectedPosition(showDeployments)
        }
    }

    private fun setSelectedPosition(showDeployments: List<DeploymentDetailView>) {
        selectedId?.let { selectedId ->
            val deploymentIndex = showDeployments.indexOf(showDeployments.find {
                when (it) {
                    is DeploymentDetailView.EdgeDeploymentView -> {
                        it.id == selectedId
                    }
                    is DeploymentDetailView.GuardianDeploymentView -> {
                        it.id == selectedId
                    }
                }
            })
            this.currentPosition = deploymentIndex
            deploymentViewPager.setCurrentItem(deploymentIndex, false)
        }
    }

    fun updateItems() {
        val showDeployments = deploymentListener?.getShowDeployments()
        showDeployments?.let {
            viewPagerAdapter.submitList(showDeployments)
            deploymentViewPager.setCurrentItem(currentPosition, false)
        }
    }

    private fun initIntent() {
        arguments?.let { selectedId = it.getInt(ARG_DEPLOYMENT_ID) }
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
        viewPagerAdapter = DeploymentViewPagerAdapter(this)
        deploymentViewPager.adapter = viewPagerAdapter
        deploymentViewPager.registerOnPageChangeCallback(providedPagerChangeCallback)
    }

    private val providedPagerChangeCallback = object : ViewPager2.OnPageChangeCallback() {
        override fun onPageSelected(position: Int) {
            if (activity is MainActivity && position < viewPagerAdapter.itemCount) {
                val detailView = viewPagerAdapter.getItemByPosition(position)
                detailView?.let {
                    this@DeploymentViewPagerFragment.currentPosition = position
                    (activity as MainActivity).moveMapIntoDeploymentMarker(
                        it.latitude,
                        it.longitude
                    )
                }
            }
        }
    }

    companion object {
        const val TAG = "DeploymentViewPagerFragment"
        private const val ARG_DEPLOYMENT_ID = "ARG_DEPLOYMENT_ID"

        @JvmStatic
        fun newInstance(id: Int) = DeploymentViewPagerFragment().apply {
            arguments = Bundle().apply {
                putInt(ARG_DEPLOYMENT_ID, id)
            }
        }
    }
}
