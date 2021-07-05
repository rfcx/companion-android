package org.rfcx.companion.view.deployment.songmeter.connect

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import org.rfcx.companion.R
import org.rfcx.companion.view.deployment.songmeter.SongMeterDeploymentProtocol

class SongMeterConnectFragment: Fragment() {

    private var deploymentProtocol: SongMeterDeploymentProtocol? = null

    override fun onAttach(context: Context) {
        super.onAttach(context)
        deploymentProtocol = (context as SongMeterDeploymentProtocol)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_songmeter_connect, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)


    }

    companion object {
        fun newInstance(): SongMeterConnectFragment {
            return SongMeterConnectFragment()
        }
    }
}
