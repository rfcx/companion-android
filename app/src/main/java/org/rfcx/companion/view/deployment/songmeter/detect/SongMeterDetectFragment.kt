package org.rfcx.companion.view.deployment.songmeter.detect

import android.Manifest
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import org.rfcx.companion.R
import org.rfcx.companion.base.ViewModelFactory
import org.rfcx.companion.databinding.FragmentSongmeterDetectBinding
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
import org.rfcx.companion.util.randomPrefixes
import org.rfcx.companion.view.deployment.songmeter.SongMeterDeploymentProtocol
import org.rfcx.companion.view.deployment.songmeter.viewmodel.SongMeterViewModel

class SongMeterDetectFragment : Fragment(), (Advertisement) -> Unit {

    private val songMeterAdapter by lazy { SongMeterAdapter(this) }

    private var deploymentProtocol: SongMeterDeploymentProtocol? = null

    private lateinit var songMeterViewModel: SongMeterViewModel

    private var selectedAdvertisement: Advertisement? = null
    private var setPrefixes = ""
    private var currentStep = 1
    private var isReadyToSet = true

    private var _binding: FragmentSongmeterDetectBinding? = null
    private val binding get() = _binding!!

    private val requestMultiplePermissions =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            permissions.entries.forEach {
                if (it.key == "android.permission.BLUETOOTH_SCAN" && it.value == false) {
                    showAlertPermission()
                }
            }
        }

    private fun setViewModel() {
        songMeterViewModel = ViewModelProvider(
            this,
            ViewModelFactory(
                requireActivity().application,
                DeviceApiHelper(DeviceApiServiceImpl(requireContext())),
                CoreApiHelper(CoreApiServiceImpl(requireContext())),
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
        _binding = FragmentSongmeterDetectBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setViewModel()
        setupTopBar()

        requestPermission()

        if (!songMeterViewModel.isBluetoothEnabled()) {
            showAlertBluetooth()
        }

        binding.stepTwoRecyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = songMeterAdapter
        }

        binding.stepFourRecyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = songMeterAdapter
        }

        setStepButton()

        observeGetConfig()
        observeSetSite()
    }

    private fun setStepButton() = binding.apply {
        stepOneButton.setOnClickListener {
            showStep(2)
            songMeterViewModel.scanBle(true)
            setObserveAdvertisement()
        }

        stepTwoYesButton.setOnClickListener {
            showStep(3)
        }

        stepTwoNoButton.setOnClickListener {
            songMeterAdapter.clear()
            selectedAdvertisement = null
            backToBeginning()
        }

        stepThreeNoButton.setOnClickListener {
            songMeterAdapter.clear()
            selectedAdvertisement = null
            backToBeginning()
        }

        stepThreeSyncButton.setOnClickListener {
            songMeterViewModel.bindConnectService(selectedAdvertisement!!.address)
            songMeterViewModel.registerGattReceiver()
            stepThreeSyncButton.isEnabled = false
            stepThreeSyncButton.text = getString(R.string.syncing)
            songMeterAdapter.clear()
            songMeterViewModel.clearAdvertisement()
            songMeterViewModel.scanBle(true)
        }

        stepFourYesButton.setOnClickListener {
            deploymentProtocol?.nextStep()
        }

        stepFourNoButton.setOnClickListener {
            songMeterAdapter.clear()
            selectedAdvertisement = null
            backToBeginning()
        }
        Unit
    }

    private fun observeSetSite() {
        songMeterViewModel.getSetSiteLiveData().observe(
            viewLifecycleOwner,
            Observer {
                when (it.status) {
                    Status.SUCCESS -> {
                        it.data?.let {
                            showStep(4)
                            songMeterViewModel.unBindConnectService()
                        }
                    }
                    Status.ERROR -> {}
                    Status.LOADING -> {}
                }
            }
        )
    }

    private fun observeGetConfig() {
        songMeterViewModel.getRequestConfigLiveData().observe(
            viewLifecycleOwner,
            Observer {
                when (it.status) {
                    Status.SUCCESS -> {
                        if (isReadyToSet) {
                            val randomPrefixes = "SM-${randomPrefixes()}"
                            setPrefixes = randomPrefixes
                            songMeterViewModel.setPrefixes(randomPrefixes)
                            deploymentProtocol?.setSongMeterId(randomPrefixes)
                            isReadyToSet = false
                        }
                    }
                    Status.ERROR -> {}
                    Status.LOADING -> {}
                }
            }
        )
    }

    private fun backToBeginning() {
        selectedAdvertisement = null
        setPrefixes = ""
        isReadyToSet = true
        songMeterViewModel.unRegisterGattReceiver()
        songMeterViewModel.unBindConnectService()
        songMeterViewModel.stopBle()
        showStep(1)
    }

    private fun showStep(step: Int) = binding.apply {
        when (step) {
            1 -> {
                currentStep = 1
                stepTwoYesButton.isEnabled = false
                stepThreeSyncButton.isEnabled = false
                stepFourYesButton.isEnabled = false
                stepThreeSyncButton.text = getString(R.string.sync)
                stepOneLayout.visibility = View.VISIBLE
                stepOneFinishLayout.visibility = View.GONE
                stepTwoLayout.visibility = View.GONE
                stepTwoFinishLayout.visibility = View.GONE
                stepThreeLayout.visibility = View.GONE
                stepThreeFinishLayout.visibility = View.GONE
                stepFourLayout.visibility = View.GONE
            }
            2 -> {
                currentStep = 2
                stepTwoLoading.show()
                stepOneLayout.visibility = View.GONE
                stepOneFinishLayout.visibility = View.VISIBLE
                stepTwoLayout.visibility = View.VISIBLE
                stepTwoLoading.visibility = View.VISIBLE
                stepTwoFinishLayout.visibility = View.GONE
                stepThreeLayout.visibility = View.GONE
                stepThreeFinishLayout.visibility = View.GONE
                stepFourLayout.visibility = View.GONE
            }
            3 -> {
                currentStep = 3
                stepOneLayout.visibility = View.GONE
                stepOneFinishLayout.visibility = View.VISIBLE
                stepTwoLayout.visibility = View.GONE
                stepTwoFinishLayout.visibility = View.VISIBLE
                stepThreeLayout.visibility = View.VISIBLE
                stepThreeFinishLayout.visibility = View.GONE
                stepFourLayout.visibility = View.GONE
            }
            4 -> {
                currentStep = 4
                stepFourLoading.show()
                stepFourTextView.text = getString(R.string.step_4_with_id, setPrefixes)
                stepOneLayout.visibility = View.GONE
                stepOneFinishLayout.visibility = View.VISIBLE
                stepTwoLayout.visibility = View.GONE
                stepTwoFinishLayout.visibility = View.VISIBLE
                stepThreeLayout.visibility = View.GONE
                stepThreeFinishLayout.visibility = View.VISIBLE
                stepFourLayout.visibility = View.VISIBLE
                stepFourLoading.visibility = View.VISIBLE
            }
        }
        Unit
    }

    private fun setupTopBar() {
        deploymentProtocol?.let {
            it.showToolbar()
            it.setToolbarTitle()
            it.setMenuToolbar(true)
        }
    }

    private fun setObserveAdvertisement() {
        songMeterViewModel.observeAdvertisement().observe(
            viewLifecycleOwner,
            Observer {
                when (it.status) {
                    Status.SUCCESS -> {
                        if (it.data != null) {
                            songMeterAdapter.items = it.data
                            if (currentStep == 2) {
                                binding.stepTwoLoading.visibility = View.GONE
                            } else if (currentStep == 4) {
                                binding.stepFourLoading.visibility = View.GONE
                            }
                            if (currentStep == 3) {
                                it.data.find { detect -> detect.serialName == selectedAdvertisement?.serialName }
                                    ?.let { filtered ->
                                        if (filtered.isReadyToPair) {
                                            binding.stepThreeSyncButton.isEnabled = true
                                        }
                                    }
                            }
                            if (currentStep == 4) {
                                it.data.find { detect -> detect.prefixes == setPrefixes }?.let {
                                    binding.stepFourYesButton.isEnabled = true
                                }
                            }
                        }
                    }
                    Status.ERROR -> {}
                    Status.LOADING -> {}
                }
            }
        )
    }

    private fun showAlertBluetooth() {
        val dialogBuilder =
            MaterialAlertDialogBuilder(requireContext(), R.style.BaseAlertDialog).apply {
                setTitle(null)
                setMessage(R.string.alert_songmeter)
                setPositiveButton(R.string.go_back) { _, _ ->
                    deploymentProtocol?.backStep()
                }
            }
        dialogBuilder.create().show()
    }

    private fun showAlertPermission() {
        val dialogBuilder =
            MaterialAlertDialogBuilder(requireContext(), R.style.BaseAlertDialog).apply {
                setTitle(null)
                setMessage(R.string.alert_bluetooth_songmeter)
                setPositiveButton(R.string.go_back) { _, _ ->
                    deploymentProtocol?.backStep()
                }
            }
        dialogBuilder.create().show()
    }

    private fun requestPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            requestMultiplePermissions.launch(
                arrayOf(
                    Manifest.permission.BLUETOOTH_SCAN,
                    Manifest.permission.BLUETOOTH_CONNECT
                )
            )
        }
    }

    override fun invoke(ads: Advertisement) {
        selectedAdvertisement = ads
        binding.stepThreePrefixesTextView.text = ads.prefixes
        binding.stepThreeSerialNumberTextView.text = ads.serialName
        binding.stepTwoYesButton.isEnabled = true
    }

    override fun onResume() {
        super.onResume()
        songMeterViewModel.registerGattReceiver()
    }

    override fun onPause() {
        super.onPause()
        songMeterViewModel.unRegisterGattReceiver()
    }

    override fun onDestroy() {
        super.onDestroy()
        songMeterViewModel.unRegisterGattReceiver()
        songMeterViewModel.unBindConnectService()
        songMeterViewModel.stopBle()
    }

    companion object {
        fun newInstance(): SongMeterDetectFragment {
            return SongMeterDetectFragment()
        }
    }
}
