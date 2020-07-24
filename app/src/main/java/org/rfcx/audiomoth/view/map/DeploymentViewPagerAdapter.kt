package org.rfcx.audiomoth.view.map

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Lifecycle
import androidx.viewpager2.adapter.FragmentStateAdapter

class DeploymentViewPagerAdapter(fragmentManager: FragmentManager, lifecycle: Lifecycle) :
    FragmentStateAdapter(fragmentManager, lifecycle) {

    var deployments = listOf<DeploymentBottomSheet>()
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    override fun getItemCount(): Int = deployments.count()

    override fun createFragment(position: Int): Fragment {
        val deployment = deployments[position]
        return MapDetailBottomSheetFragment.newInstance(deployment.id, deployment.device)
    }
}