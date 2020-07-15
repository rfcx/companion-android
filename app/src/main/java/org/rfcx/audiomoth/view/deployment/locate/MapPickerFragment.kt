package org.rfcx.audiomoth.view.deployment.locate

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import org.rfcx.audiomoth.R

class MapPickerFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_map_picker, container, false)
    }

    companion object {
        @JvmStatic
        fun newInstance() = MapPickerFragment()
    }
}