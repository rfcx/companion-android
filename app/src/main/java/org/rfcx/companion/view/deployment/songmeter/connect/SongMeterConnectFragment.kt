package org.rfcx.companion.view.deployment.songmeter.connect

import android.content.Context
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.RequiresApi
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.android.synthetic.main.fragment_songmeter_connect.*
import org.rfcx.companion.R
import org.rfcx.companion.base.ViewModelFactory
import org.rfcx.companion.entity.songmeter.Advertisement
import org.rfcx.companion.repo.api.DeviceApiHelper
import org.rfcx.companion.repo.api.DeviceApiServiceImpl
import org.rfcx.companion.repo.ble.BleDetectService
import org.rfcx.companion.repo.ble.BleHelper
import org.rfcx.companion.repo.local.LocalDataHelper
import org.rfcx.companion.util.Status
import org.rfcx.companion.view.deployment.songmeter.SongMeterDeploymentProtocol
import org.rfcx.companion.view.deployment.songmeter.SongMeterViewModel

class SongMeterConnectFragment: Fragment(), (Advertisement) -> Unit {

    private var advertisement: Advertisement? = null

    private val songMeterAdapter by lazy { SongMeterAdapter(this) }

    private var deploymentProtocol: SongMeterDeploymentProtocol? = null

    private lateinit var songMeterViewModel: SongMeterViewModel

    private fun setViewModel() {
        songMeterViewModel = ViewModelProvider(
            this,
            ViewModelFactory(
                requireActivity().application,
                DeviceApiHelper(DeviceApiServiceImpl()),
                LocalDataHelper(),
                BleHelper(BleDetectService(requireContext()))
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
        setObserveAdvertisement()
        songMeterViewModel.scanBle(true)

        songMeterRecyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = songMeterAdapter
        }

        retryScanButton.setOnClickListener {
            songMeterViewModel.scanBle(true)
        }
    }

    private fun setupTopBar() {
        deploymentProtocol?.let {
            it.showToolbar()
            it.setToolbarTitle()
        }
    }

    private fun setObserveAdvertisement() {
        songMeterViewModel.observeAdvertisement().observe(viewLifecycleOwner, Observer {
            when (it.status) {
                Status.LOADING -> {
                    retryScanButton.isEnabled = false
                    songMeterLoading.show()
                }
                Status.SUCCESS -> {
                    if (it.data == null) {
                        retryScanButton.isEnabled = true
                        songMeterLoading.hide()
                    } else {
                        songMeterSuggestTextView.visibility = View.GONE
                        songMeterAdapter.items = listOf(it.data)
                    }
                }
                Status.ERROR -> { }
            }
        })
    }

    private fun enableConnectButton() {
        connectSongMeterButton.isEnabled = true
    }

    override fun invoke(ads: Advertisement) {
        advertisement = ads
        enableConnectButton()
    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    override fun onPause() {
        super.onPause()

        songMeterViewModel.stopBle()
    }

    companion object {
        fun newInstance(): SongMeterConnectFragment {
            return SongMeterConnectFragment()
        }
    }
}
