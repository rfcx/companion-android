package org.rfcx.companion.view.deployment.songmeter.connect

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import org.rfcx.companion.R
import org.rfcx.companion.base.ViewModelFactory
import org.rfcx.companion.entity.songmeter.Advertisement
import org.rfcx.companion.repo.api.DeviceApiHelper
import org.rfcx.companion.repo.api.DeviceApiServiceImpl
import org.rfcx.companion.repo.ble.BleConnectDelegate
import org.rfcx.companion.repo.ble.BleDetectService
import org.rfcx.companion.repo.ble.BleHelper
import org.rfcx.companion.repo.local.LocalDataHelper
import org.rfcx.companion.view.deployment.songmeter.SongMeterDeploymentProtocol
import org.rfcx.companion.view.deployment.songmeter.viewmodel.SongMeterViewModel

class SongMeterConnectFragment : Fragment() {

    private var advertisement: Advertisement? = null

    private var deploymentProtocol: SongMeterDeploymentProtocol? = null

    private lateinit var songMeterViewModel: SongMeterViewModel

    private fun setViewModel() {
        songMeterViewModel = ViewModelProvider(
            this,
            ViewModelFactory(
                requireActivity().application,
                DeviceApiHelper(DeviceApiServiceImpl()),
                LocalDataHelper(),
                BleHelper(BleDetectService(requireContext()), BleConnectDelegate(requireContext()))
            )
        ).get(SongMeterViewModel::class.java)
    }

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

        setViewModel()
        setupTopBar()
        getArgument()

        songMeterViewModel.registerGattReceiver()
        songMeterViewModel.bindConnectService(advertisement!!.address)
    }

    private fun getArgument() {
        arguments?.let {
            advertisement = it.getSerializable(ADVERTISEMENT) as Advertisement
        }
    }

    private fun setupTopBar() {
        deploymentProtocol?.let {
            it.showToolbar()
            it.setToolbarTitle()
        }
    }

    override fun onPause() {
        super.onPause()
        songMeterViewModel.unRegisterGattReceiver()
    }

    override fun onDestroy() {
        super.onDestroy()
        songMeterViewModel.unBindConnectService()
    }

    companion object {
        private const val ADVERTISEMENT = "ARG_ADVERTISEMENT"

        fun newInstance(advertisement: Advertisement): SongMeterConnectFragment {
            return SongMeterConnectFragment().apply {
                arguments = Bundle().apply {
                    putSerializable(ADVERTISEMENT, advertisement)
                }
            }
        }
    }
}
