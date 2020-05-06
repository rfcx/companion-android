package org.rfcx.audiomoth.view.configure

import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import kotlinx.android.synthetic.main.fragment_battery_level_bottom_sheet.*
import kotlinx.android.synthetic.main.fragment_perform_battery.*
import org.rfcx.audiomoth.R
import org.rfcx.audiomoth.util.showCommonDialog
import org.rfcx.audiomoth.util.toDateTimeString
import java.util.*

class PerformBatteryFragment : Fragment(), BatteryLevelListener {

    private val batteryLevelFragment by lazy { BatteryLevelBottomSheetFragment(this) }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_perform_battery, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        checkBatteryButton.setOnClickListener {
            activity?.showCommonDialog(null,
                getString(
                    R.string.msg_ask_count_flashes
                ), DialogInterface.OnClickListener { dialog, _ ->
                    batteryLevelFragment.show(
                        childFragmentManager,
                        BatteryLevelBottomSheetFragment.TAG
                    )
                    dialog.dismiss()
                }
            )
        }

        skipButton.setOnClickListener {
            predictBatteryLife()
        }
    }

    override fun onSelectedBatteryLevel(level: Int) {
        batteryLevelFragment.dismiss()
        predictBatteryLife(level)
    }

    private fun predictBatteryLife(batteryLv: Int = 3) {
        // TODO: Update later it is mockup!
        val today = Calendar.getInstance()
        val mockPredict = Date(today.timeInMillis + (batteryLv * 24 * 60 * 60 * 1000))
        activity?.showCommonDialog(null,
            getString(
                R.string.format_confirm_perform_battery,
                mockPredict.toDateTimeString()
            ), DialogInterface.OnClickListener { dialog, _ ->
                // TODO: Open Deploy Page
                dialog.dismiss()
            }
        )
    }

    companion object {
        const val TAG = "PerformBatteryFragment"
    }
}

interface BatteryLevelListener {
    fun onSelectedBatteryLevel(level: Int)
}

class BatteryLevelBottomSheetFragment(private val listener: BatteryLevelListener) :
    BottomSheetDialogFragment() {
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_battery_level_bottom_sheet, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setView()
    }

    private fun setView() {
        batteryLv1Button.setOnClickListener { listener.onSelectedBatteryLevel(1) }
        batteryLv2Button.setOnClickListener { listener.onSelectedBatteryLevel(2) }
        batteryLv3Button.setOnClickListener { listener.onSelectedBatteryLevel(3) }
    }

    companion object {
        const val TAG = "BatteryLevelDialog"
    }
}