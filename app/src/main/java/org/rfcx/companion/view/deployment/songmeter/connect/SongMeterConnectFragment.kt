package org.rfcx.companion.view.deployment.songmeter.connect

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.RequiresApi
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.android.synthetic.main.fragment_songmeter_connect.*
import org.rfcx.companion.R
import org.rfcx.companion.entity.songmeter.Advertisement
import org.rfcx.companion.util.songmeter.AdvertisementUtils
import org.rfcx.companion.view.deployment.songmeter.SongMeterDeploymentProtocol

class SongMeterConnectFragment: Fragment(), (Advertisement) -> Unit {

    private lateinit var bluetoothAdapter: BluetoothAdapter
    private var isScanning = false

    private var deviceName: String? = null
    private var serialNumber: String? = null
    private var prefixes: String? = null
    private var address: String? = null
    private val advertisementUtils by lazy { AdvertisementUtils() }

    private var advertisement: Advertisement? = null

    private val songMeterAdapter by lazy { SongMeterAdapter(this) }

    private var deploymentProtocol: SongMeterDeploymentProtocol? = null

    private val scannerCallback = @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    object : ScanCallback() {
        override fun onScanFailed(errorCode: Int) {
            super.onScanFailed(errorCode)
        }

        override fun onBatchScanResults(results: MutableList<ScanResult>?) {
            super.onBatchScanResults(results)
        }

        override fun onScanResult(callbackType: Int, result: ScanResult?) {
            super.onScanResult(callbackType, result)

            if (result != null) {
                if (result.device.address.split(":").joinToString("").substring(0,6) == "9C25BE") {
                    address = result.device.address
                    advertisementUtils.convertAdvertisementToObject(result.scanRecord!!.bytes)
                    serialNumber = advertisementUtils.getSerialNumber()
                    prefixes = advertisementUtils.getPrefixes()
                    addDetectedRecorder()
                }
            }
        }
    }

    private fun addDetectedRecorder() {
        if (serialNumber != null && prefixes != null) {
            songMeterSuggestTextView.visibility = View.GONE
            songMeterAdapter.items  = listOf(Advertisement(prefixes!!, "SMM${serialNumber!!}"))
        }
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

        bluetoothAdapter = (requireContext().getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager).adapter

        setupTopBar()
        scanBLEDevice(true)

        songMeterRecyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = songMeterAdapter
        }

        retryScanButton.setOnClickListener {
            scanBLEDevice(true)
        }
    }

    private fun setupTopBar() {
        deploymentProtocol?.let {
            it.showToolbar()
            it.setToolbarTitle()
        }
    }

    private fun scanBLEDevice(isEnable: Boolean) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            val scanner = bluetoothAdapter.bluetoothLeScanner

            if (isEnable) {
                Handler(Looper.getMainLooper()).postDelayed({
                    isScanning = false
                    retryScanButton.isEnabled = true
                    scanner.stopScan(scannerCallback)
                    songMeterLoading.hide()
                }, 10000)

                retryScanButton.isEnabled = false
                songMeterLoading.show()
                isScanning = true
                scanner.startScan(scannerCallback)
            } else {
                isScanning = false
                scanner.stopScan(scannerCallback)
            }
        }
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

        scanBLEDevice(false)
    }

    companion object {
        fun newInstance(): SongMeterConnectFragment {
            return SongMeterConnectFragment()
        }
    }
}
