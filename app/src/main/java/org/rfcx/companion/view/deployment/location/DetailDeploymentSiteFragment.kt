package org.rfcx.companion.view.deployment.location

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import org.rfcx.companion.R

class DetailDeploymentSiteFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_detail_deployment_site, container, false)
    }

    companion object {
        private const val ARG_SITE_ID = "ARG_SITE_ID"
        private const val ARG_SITE_NAME = "ARG_SITE_NAME"

        @JvmStatic
        fun newInstance(id: Int, name: String?) =
            DetailDeploymentSiteFragment().apply {
                arguments = Bundle().apply {
                    putInt(ARG_SITE_ID, id)
                    putString(ARG_SITE_NAME, name)
                }
            }
    }
}
