package org.rfcx.companion.view.deployment.songmeter.connect

import android.content.Context
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.observe
import kotlinx.android.synthetic.main.fragment_songmeter_connect.*
import kotlinx.android.synthetic.main.fragment_songmeter_detect.*
import kotlinx.android.synthetic.main.fragment_songmeter_detect.songMeterSuggestTextView
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
import org.rfcx.companion.util.randomPrefixes
import org.rfcx.companion.view.deployment.songmeter.SongMeterDeploymentProtocol
import org.rfcx.companion.view.deployment.songmeter.viewmodel.SongMeterViewModel

class SongMeterConnectFragment : Fragment(), TextWatcher {

    private var advertisement: Advertisement? = null

    private var deploymentProtocol: SongMeterDeploymentProtocol? = null

    private lateinit var songMeterViewModel: SongMeterViewModel

    private var isSettingPrefixes = false

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
        return inflater.inflate(R.layout.fragment_songmeter_connect, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setViewModel()
        setupTopBar()
        getArgument()
        observeGattConnection()
        setupRecorderId()
        observeSetSite()
        observeGetConfig()

        finishSongMeterButton.setOnClickListener {
            isSettingPrefixes = true
            songMeterViewModel.setPrefixes(songMeterSiteIdEditText.text.toString())
            deploymentProtocol?.setSongMeterId(songMeterSiteIdEditText.text.toString())
        }

        songMeterViewModel.bindConnectService(advertisement!!.address)
        songMeterViewModel.registerGattReceiver()

        songMeterSiteIdEditText.addTextChangedListener(this)
        randomPrefixes.setOnClickListener{
            songMeterSiteIdEditText.setText(randomPrefixes())
        }
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

    private fun observeSetSite() {
        songMeterViewModel.getSetSiteLiveData().observe(viewLifecycleOwner, Observer {
            when (it.status) {
                Status.LOADING -> {
                    disableUI()
                }
                Status.SUCCESS -> {
                    it.data?.let { result ->
                        if (result) {
                            deploymentProtocol?.nextStep()
                        }
                    }
                }
            }
        })
    }

    private fun observeGetConfig() {
        songMeterViewModel.getRequestConfigLiveData().observe(viewLifecycleOwner, Observer {
            when (it.status) {
                Status.LOADING -> {
                    disableUI()
                }
                Status.SUCCESS -> {
                    enableUI()
                }
            }
        })
    }

    private fun disableUI() {
        finishSongMeterButton.isEnabled = false
        songMeterSiteIdEditText.isEnabled = false
    }

    private fun enableUI() {
        if (!isSettingPrefixes) {
            finishSongMeterButton.isEnabled = true
            songMeterSiteIdEditText.isEnabled = true
        }
    }

    private fun observeGattConnection() {
        songMeterViewModel.observeGattConnection().observe(viewLifecycleOwner, Observer {
            when (it.status) {
                Status.LOADING -> { }
                Status.SUCCESS -> {
                    if (it.data != null) {
                        if (it.data == true) {
                            hideSuggestMessage()
                            showConfigUI()
                        } else {
                            hideConfigUI()
                            showSuggestMessage()
                        }
                    }
                }
                Status.ERROR -> { }
            }
        })
    }

    private fun setupRecorderId() {
        songMeterSiteIdEditText.setText(advertisement!!.prefixes)
    }

    private fun showConfigUI() {
        songMeterConnectTitle.visibility = View.VISIBLE
        songMeterConnectDesc.visibility = View.VISIBLE
        prefixedLayout.visibility = View.VISIBLE
        randomPrefixes.visibility = View.VISIBLE
        songMeterSiteIdEditText.visibility = View.VISIBLE
    }

    private fun hideConfigUI() {
        songMeterConnectTitle.visibility = View.GONE
        songMeterConnectDesc.visibility = View.GONE
        prefixedLayout.visibility = View.GONE
        randomPrefixes.visibility = View.GONE
        songMeterSiteIdEditText.visibility = View.GONE
    }

    private fun showSuggestMessage() {
        songMeterSuggestTextView.visibility = View.VISIBLE
    }

    private fun hideSuggestMessage() {
        songMeterSuggestTextView.visibility = View.GONE
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

    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
        // nothing to do here
    }

    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
        val isRequired = isIdPassTheRequirement(songMeterSiteIdEditText.text.toString())
        if (isRequired.first) {
            finishSongMeterButton.isEnabled = true
            prefixedLayout.error = null
        } else {
            finishSongMeterButton.isEnabled = false
            prefixedLayout.error = isRequired.second
        }

    }

    override fun afterTextChanged(s: Editable?) {
        // nothing to do here
    }

    private val lowercase = "(.*[a-z].*)"
    private val symbol = "(.*[:?!@#$%^&*();/-].*)"
    private fun isIdPassTheRequirement(id: String?): Pair<Boolean, String> {
        if (id.isNullOrBlank()) return Pair(false, getString(R.string.empty))
        if (id.matches(Regex(lowercase))) return Pair(false, getString(R.string.lower_not_allowed))
        if (id.matches(Regex(symbol))) return Pair(false, getString(R.string.symbole_not_allowed))
        if (id.length != 12) return Pair(false, getString(R.string.req_12_char))
        return Pair(true, "")
    }
}
