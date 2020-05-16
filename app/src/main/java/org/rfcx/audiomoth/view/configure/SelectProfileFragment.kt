package org.rfcx.audiomoth.view.configure


import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import org.rfcx.audiomoth.R
import org.rfcx.audiomoth.view.DeploymentActivity
import org.rfcx.audiomoth.view.DeploymentActivity.Companion.CONFIGURE_FRAGMENT
import org.rfcx.audiomoth.view.DeploymentActivity.Companion.LOCATION_FRAGMENT
import org.rfcx.audiomoth.view.DeploymentActivity.Companion.SELECT_PROFILE_FRAGMENT
import org.rfcx.audiomoth.view.DeploymentProtocol

class SelectProfileFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_select_profile, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        (activity as DeploymentProtocol).setLastPageInStep(false, CONFIGURE_FRAGMENT)
    }

    companion object {
        fun newInstance(): SelectProfileFragment {
            return SelectProfileFragment()
        }
    }
}
