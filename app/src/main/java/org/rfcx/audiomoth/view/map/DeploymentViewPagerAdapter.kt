package org.rfcx.audiomoth.view.map

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Lifecycle
import androidx.viewpager2.adapter.FragmentStateAdapter
import org.rfcx.audiomoth.entity.Deployment

class DeploymentViewPagerAdapter(fragmentManager: FragmentManager, lifecycle: Lifecycle) :
    FragmentStateAdapter(fragmentManager, lifecycle) {

    var deployments = listOf<Deployment>()
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    override fun getItemCount(): Int = deployments.count()

    override fun createFragment(position: Int): Fragment {
        return MapDetailBottomSheetFragment.newInstance(deployments[position].id)
    }
}