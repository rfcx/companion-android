package org.rfcx.companion.view.deployment.songmeter.detect

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.android.synthetic.main.fragment_songmeter_detect.*
import org.rfcx.companion.R
import org.rfcx.companion.base.ViewModelFactory
import org.rfcx.companion.entity.songmeter.Advertisement
import org.rfcx.companion.repo.api.CoreApiHelper
import org.rfcx.companion.repo.api.CoreApiServiceImpl
import org.rfcx.companion.repo.api.DeviceApiHelper
import org.rfcx.companion.repo.api.DeviceApiServiceImpl
import org.rfcx.companion.repo.ble.BleConnectDelegate
import org.rfcx.companion.repo.ble.BleDetectService
import org.rfcx.companion.repo.ble.BleHelper
import org.rfcx.companion.repo.local.LocalDataHelper
import org.rfcx.companion.util.Status
import org.rfcx.companion.view.deployment.songmeter.SongMeterDeploymentProtocol
import org.rfcx.companion.view.deployment.songmeter.viewmodel.SongMeterViewModel

class SongMeterDetectFragment : Fragment(), (Advertisement) -> Unit {

    private val songMeterAdapter by lazy { SongMeterAdapter(this) }

    private var deploymentProtocol: SongMeterDeploymentProtocol? = null

    private lateinit var songMeterViewModel: SongMeterViewModel

    private fun setViewModel() {
        songMeterViewModel = ViewModelProvider(
            this,
            ViewModelFactory(
                requireActivity().application,
                DeviceApiHelper(DeviceApiServiceImpl()),
                CoreApiHelper(CoreApiServiceImpl()),
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
        return inflater.inflate(R.layout.fragment_songmeter_detect, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setViewModel()
        setupTopBar()
        setObserveAdvertisement()
        if (songMeterViewModel.isBluetoothEnabled()) {
            songMeterViewModel.scanBle(true)
        } else {
            showAlertBluetooth()
        }

        songMeterRecyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = songMeterAdapter
        }
    }

    private fun setupTopBar() {
        deploymentProtocol?.let {
            it.showToolbar()
            it.setToolbarTitle()
            it.setMenuToolbar(true)
        }
    }

    private fun setObserveAdvertisement() {
        songMeterViewModel.observeAdvertisement().observe(viewLifecycleOwner, Observer {
            when (it.status) {
                Status.LOADING -> {
                    songMeterLoading.show()
                }
                Status.SUCCESS -> {
                    if (it.data == null) {
                        songMeterLoading.hide()
                    } else {
                        songMeterSuggestTextView.visibility = View.GONE
                        songMeterAdapter.items = it.data
                    }
                }
                Status.ERROR -> {}
            }
        })
    }

    private fun showAlertBluetooth() {
        val dialogBuilder: AlertDialog.Builder =
            AlertDialog.Builder(requireContext()).apply {
                setTitle(null)
                setMessage(R.string.alert_songmeter)
                setPositiveButton(R.string.go_back) { _, _ ->
                    deploymentProtocol?.backStep()
                }
            }
        dialogBuilder.create().show()
    }

    override fun invoke(ads: Advertisement) {
        deploymentProtocol?.redirectToConnectSongMeter(ads)
        songMeterViewModel.stopBle()
    }

    override fun onPause() {
        super.onPause()
        songMeterViewModel.stopBle()
    }

    override fun onDestroy() {
        super.onDestroy()
        songMeterViewModel.stopBle()
    }

    companion object {
        fun newInstance(): SongMeterDetectFragment {
            return SongMeterDetectFragment()
        }
    }
}