package org.rfcx.audiomoth.view.map

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.MarginPageTransformer
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.bottomsheet.BottomSheetDialog
import io.realm.Realm
import kotlinx.android.synthetic.main.buttom_sheet_delete_layout.view.*
import kotlinx.android.synthetic.main.fragment_deployment_view_pager.*
import org.rfcx.audiomoth.DeploymentListener
import org.rfcx.audiomoth.MainActivity
import org.rfcx.audiomoth.R
import org.rfcx.audiomoth.entity.DeploymentState
import org.rfcx.audiomoth.localdb.EdgeDeploymentDb
import org.rfcx.audiomoth.localdb.LocateDb
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
    private val edgeDeploymentDb by lazy {
        EdgeDeploymentDb(Realm.getInstance(RealmHelper.migrationConfig()))
    }
    private val locateDb by lazy {
        LocateDb(Realm.getInstance(RealmHelper.migrationConfig()))
    }
    private var deploymentListener: DeploymentListener? = null
    private lateinit var viewPagerAdapter: DeploymentViewPagerAdapter
    private var selectedId: Int? = null // selected deployment id
    private var currentPosition: Int = 0
    private var edgeDeploymentViewId: Int? = null
    private var locateId: Int? = null
    private lateinit var deleteDialog: BottomSheetDialog

    override fun onAttach(context: Context) {
        super.onAttach(context)
        deploymentListener = context as DeploymentListener
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initIntent()
        setupDeleteDialog()
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
    override fun onClickedMoreIcon(edgeDeploymentView: DeploymentDetailView.EdgeDeploymentView) {
        locateId = locateDb.getDeleteLocateId(
            edgeDeploymentView.locationName,
            edgeDeploymentView.latitude,
            edgeDeploymentView.longitude
        )
        edgeDeploymentViewId = edgeDeploymentView.id
        deleteDialog.show()
    }

    override fun onClickedEdgeDeploymentDetail(edgeDeploymentView: DeploymentDetailView.EdgeDeploymentView) {
        context?.let {
            this@DeploymentViewPagerFragment.selectedId = edgeDeploymentView.id
            val isReadyToUpload = edgeDeploymentView.state == DeploymentState.Edge.ReadyToUpload.key
            if (isReadyToUpload) {
                DeploymentDetailActivity.startActivity(it, edgeDeploymentView.id)
            } else {
                EdgeDeploymentActivity.startActivity(
                    it,
                    edgeDeploymentView.id,
                    MainActivity.CREATE_DEPLOYMENT_REQUEST_CODE
                )
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

    private fun setupDeleteDialog() {
        val bottomSheetView =
            layoutInflater.inflate(R.layout.buttom_sheet_delete_layout, null)

        bottomSheetView.menuDelete.setOnClickListener { onDeleteLocationOfNoDeployment() }

        context?.let { deleteDialog = BottomSheetDialog(it) }
        deleteDialog.setContentView(bottomSheetView)
    }

    private fun onDeleteLocationOfNoDeployment() {
        if (edgeDeploymentViewId != null && locateId != null) {
            locateDb.deleteLocate(locateId!!)
            edgeDeploymentDb.deleteDeployment(edgeDeploymentViewId!!)
            deleteDialog.dismiss()
        } else {
            Toast.makeText(context, R.string.error_has_occurred, Toast.LENGTH_SHORT).show()
            deleteDialog.dismiss()
        }
    }

    private fun setViewPagerAdapter() {
        val showDeployments = deploymentListener?.getShowDeployments()
        if (showDeployments != null) {
            viewPagerAdapter.submitList(showDeployments) // adapter update items
            setSelectedPosition(showDeployments)
        }
    }

    private fun setSelectedPosition(showDeployments: List<DeploymentDetailView>) {
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
        // if position = -1 meant have no item in list
        this.currentPosition = findCorrectItemIndex(deploymentIndex)
        deploymentViewPager.setCurrentItem(deploymentIndex, false)
    }

    private fun findCorrectItemIndex(deploymentIndex: Int): Int {
        return if (deploymentIndex != -1) {
            deploymentIndex
        } else {
            if (this.currentPosition > 0) this.currentPosition - 1 else 0
        }
    }

    fun updateItems() {
        val showDeployments = deploymentListener?.getShowDeployments()
        showDeployments?.let {
            viewPagerAdapter.submitList(it)
            setSelectedPosition(it)
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
        (deploymentViewPager.getChildAt(0) as RecyclerView).overScrollMode = RecyclerView.OVER_SCROLL_NEVER
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
