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
import kotlinx.android.synthetic.main.fragment_deployment_view_pager.*
import org.rfcx.audiomoth.DeploymentListener
import org.rfcx.audiomoth.MainActivity
import org.rfcx.audiomoth.R

class DeploymentViewPagerFragment : Fragment() {

    private var deploymentListener: DeploymentListener? = null
    private var id: Int? = null
    private lateinit var viewPagerAdapter: DeploymentViewPagerAdapter

    override fun onAttach(context: Context) {
        super.onAttach(context)
        deploymentListener = context as DeploymentListener
    }

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

        initAdapter()
        setViewPagerAdapter()

    }

    private fun setViewPagerAdapter() {
        val showDeployments = deploymentListener?.getShowDeployments()
        if (showDeployments != null) {
            id?.let {
                viewPagerAdapter.deployments = showDeployments

                val deploymentIndex = showDeployments.indexOf(showDeployments.find { it.id == this.id })
                deploymentViewPager.post {
                    deploymentViewPager.setCurrentItem(deploymentIndex, false)
                }
            }
        }
    }

    private fun initIntent() {
        arguments?.let {
            id = it.getInt(ARG_ID)
        }
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
