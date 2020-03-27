package org.rfcx.audiomoth.view.configure

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import kotlinx.android.synthetic.main.fragment_verify_sync.*
import org.rfcx.audiomoth.R
import org.rfcx.audiomoth.view.CreateStreamActivity
import org.rfcx.audiomoth.view.dashboard.DashboardStreamActivity

class VerifySyncFragment : Fragment() {

    lateinit var listener: ConfigureListener

    override fun onAttach(context: Context) {
        super.onAttach(context)
        listener = (context as ConfigureListener)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_verify_sync, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        greenButton.setOnClickListener {
            if (arguments?.containsKey(CreateStreamActivity.DEVICE_ID) == true) {
                arguments?.let {
                    val deviceId = it.getString(CreateStreamActivity.DEVICE_ID)
                    if (deviceId != null) {
                        context?.let { it1 -> DashboardStreamActivity.startActivity(it1, deviceId) }
                    }
                }
            }
        }

        redButton.setOnClickListener {
            listener.openSync()
        }
    }
    companion object {
        fun newInstance(deviceId: String): VerifySyncFragment {
            return VerifySyncFragment().apply {
                arguments = Bundle().apply {
                    putString(CreateStreamActivity.DEVICE_ID, deviceId)
                }
            }
        }
    }
}
