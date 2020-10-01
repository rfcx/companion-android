package org.rfcx.audiomoth.view.profile.locationgroup

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.android.synthetic.main.fragment_location_group.*
import org.rfcx.audiomoth.R

class LocationGroupFragment : Fragment() {
    private val locationGroupAdapter by lazy { LocationGroupAdapter() }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_location_group, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        locationGroupRecyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = locationGroupAdapter
        }

        locationGroupAdapter.items = listOf("Location Group 01", "Location Group 02", "Location Group 03")
    }

    companion object {
        @JvmStatic
        fun newInstance() = LocationGroupFragment()
    }
}
