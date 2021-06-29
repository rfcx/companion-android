package org.rfcx.companion.view.deployment.songmeter

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import org.rfcx.companion.R

class SongMeterCheckListFragment : Fragment() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_song_meter_check_list, container, false)
    }

    companion object {
        @JvmStatic
        fun newInstance() = SongMeterCheckListFragment()
    }
}
