package org.rfcx.companion.view.deployment.guardian.signal

import android.content.Context
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import kotlinx.android.synthetic.main.fragment_guardian_signal.*
import org.rfcx.companion.R
import org.rfcx.companion.connection.socket.AdminSocketManager
import org.rfcx.companion.connection.socket.GuardianSocketManager
import org.rfcx.companion.entity.Screen
import org.rfcx.companion.util.Analytics
import org.rfcx.companion.view.deployment.guardian.GuardianDeploymentProtocol

class GuardianSignalFragment : Fragment() {
    private val listOfSignal by lazy {
        listOf(signalStrength1, signalStrength2, signalStrength3, signalStrength4)
    }

    private val listOfTag by lazy {
        listOf(
            satBadSignalQuality,
            satOKSignalQuality,
            satGoodSignalQuality,
            satPerfectSignalQuality
        )
    }

    private var isWaitingSpeedTest = false
    private var downloadSpeed: Double? = null
    private var uploadSpeed: Double? = null

    private var deploymentProtocol: GuardianDeploymentProtocol? = null

    private val analytics by lazy { context?.let { Analytics(it) } }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        deploymentProtocol = (context as GuardianDeploymentProtocol)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_guardian_signal, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        deploymentProtocol?.let {
            it.setToolbarSubtitle()
            it.setMenuToolbar(false)
            it.showToolbar()
            it.setToolbarTitle()
        }

        retrieveSimModule()
        retrieveSatModule()

        finishButton.setOnClickListener {
            analytics?.trackClickNextEvent(Screen.GUARDIAN_SIGNAL.id)
            deploymentProtocol?.nextStep()
        }
    }

    private fun retrieveSimModule() {
        AdminSocketManager.pingBlob.observe(viewLifecycleOwner, Observer {
            val hasSim = deploymentProtocol?.getSimDetected()
            if (hasSim != null && hasSim) {
                simDetectionCheckbox.isChecked = true
                cellSignalLayout.visibility = View.VISIBLE
                cellDataTransferLayout.visibility = View.VISIBLE
            } else {
                simDetectionCheckbox.isChecked = false
                cellSignalLayout.visibility = View.GONE
                cellDataTransferLayout.visibility = View.GONE
            }

            val cellStrength = deploymentProtocol?.getNetwork()
            if (cellStrength == null) {
                showCellSignalStrength(SignalState.NONE)
                signalValue.text = "failed to retrieve"
            } else {
                when {
                    cellStrength > -70 -> showCellSignalStrength(SignalState.MAX)
                    cellStrength > -90 -> showCellSignalStrength(SignalState.HIGH)
                    cellStrength > -110 -> showCellSignalStrength(SignalState.NORMAL)
                    cellStrength > -130 -> showCellSignalStrength(SignalState.LOW)
                    else -> showCellSignalStrength(SignalState.NONE)
                }
                signalValue.text = getString(R.string.signal_value, cellStrength)
            }

            val speedTest = deploymentProtocol?.getSpeedTest()
            val tempDownload = speedTest?.downloadSpeed
            val tempUpload = speedTest?.uploadSpeed
            val hasConnection = speedTest?.hasConnection
            val isFailed = speedTest?.isFailed

            if (hasConnection != null && hasConnection) {
                showSpeedTest()
            } else {
                hideHideSpeedTest()
            }

            if (isWaitingSpeedTest && isSpeedTestChanged(
                    downloadSpeed,
                    uploadSpeed,
                    tempDownload,
                    tempUpload
                )
            ) {
                isWaitingSpeedTest = false

            }

            when {
                isFailed != null && isFailed -> {
                    cellDownloadDataTransferValues.text = "failed to retrieve"
                    cellUploadDataTransferValues.text = "failed to retrieve"
                }
                isWaitingSpeedTest -> {
                    cellDownloadDataTransferValues.text = "waiting for testing"
                    cellUploadDataTransferValues.text = "waiting for testing"
                }
                !isWaitingSpeedTest -> {
                    if (downloadSpeed == null) {
                        cellDownloadDataTransferValues.text = "run the test"
                    } else {
                        cellDownloadDataTransferValues.text =
                            "${String.format("%.2f", downloadSpeed)} kb/s download"
                    }
                    if (uploadSpeed == null) {
                        cellUploadDataTransferValues.text = "run the test"
                    } else {
                        cellUploadDataTransferValues.text =
                            "${String.format("%.2f", uploadSpeed)} kb/s upload"
                    }
                }
            }
        })

        cellDataTransferButton.setOnClickListener {
            GuardianSocketManager.runSpeedTest()
            isWaitingSpeedTest = true
        }
    }

    private fun isSpeedTestChanged(
        currentDownload: Double?,
        currentUpload: Double?,
        newDownload: Double?,
        newUpload: Double?
    ): Boolean {
        if (currentDownload == newDownload || newDownload == null || newDownload == -1.0) return false
        downloadSpeed = newDownload
        if (currentUpload == newUpload || newUpload == null || newUpload == -1.0) return false
        uploadSpeed = newUpload
        return true
    }

    private fun showSpeedTest() {
        noCellConnectionText.visibility = View.GONE
        cellDownloadDataTransferValues.visibility = View.VISIBLE
        cellUploadDataTransferValues.visibility = View.VISIBLE
        cellDataTransferButton.visibility = View.VISIBLE
    }

    private fun hideHideSpeedTest() {
        noCellConnectionText.visibility = View.VISIBLE
        cellDownloadDataTransferValues.visibility = View.GONE
        cellUploadDataTransferValues.visibility = View.GONE
        cellDataTransferButton.visibility = View.GONE
    }

    private fun retrieveSatModule() {
        val hasSatModule = deploymentProtocol?.getSatId()
        if (hasSatModule != null) {
            satDetectionCheckbox.isChecked = true
            satSignalLayout.visibility = View.VISIBLE
        } else {
            satDetectionCheckbox.isChecked = false
            satSignalLayout.visibility = View.GONE
        }

        val swmStrength = deploymentProtocol?.getSwmNetwork()
        if (swmStrength == null) {
            showCellSignalStrength(SignalState.NONE)
            signalValue.text = "failed to retrieve"
        } else {
            when {
                swmStrength < -104 -> showSatSignalTagStrength(SignalState.MAX)
                swmStrength < -100 -> showSatSignalTagStrength(SignalState.HIGH)
                swmStrength < -97 -> showSatSignalTagStrength(SignalState.NORMAL)
                swmStrength < -93 -> showSatSignalTagStrength(SignalState.LOW)
                else -> showSatSignalTagStrength(SignalState.LOW)
            }
            satSignalValues.text = getString(R.string.signal_value, swmStrength)
        }
    }

    private fun showCellSignalStrength(state: SignalState) {
        listOfSignal.forEachIndexed { index, view ->
            if (index < state.value) {
                (view.background as GradientDrawable).setBackground(
                    requireContext(),
                    R.color.signal_filled
                )
            } else {
                (view.background as GradientDrawable).setBackground(requireContext(), R.color.white)
            }
        }
    }

    private fun showSatSignalTagStrength(state: SignalState) {
        listOfTag.forEachIndexed { index, view ->
            if ((index + 1) == state.value) {
                view.visibility = View.VISIBLE
            } else {
                view.visibility = View.GONE
            }
        }
    }

    override fun onResume() {
        super.onResume()
        analytics?.trackScreen(Screen.GUARDIAN_SIGNAL)
    }

    companion object {

        private enum class SignalState(val value: Int) {
            NONE(0), LOW(1), NORMAL(2), HIGH(3), MAX(4)
        }

        private fun GradientDrawable.setBackground(context: Context, color: Int) {
            this.setColor(ContextCompat.getColor(context, color))
        }

        fun newInstance(): GuardianSignalFragment = GuardianSignalFragment()
    }
}
