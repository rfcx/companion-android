package org.rfcx.audiomoth.view.dashboard

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.android.synthetic.main.activity_dashboard.*
import org.rfcx.audiomoth.R
import org.rfcx.audiomoth.entity.Stream
import org.rfcx.audiomoth.util.Firestore
import org.rfcx.audiomoth.view.CreateStreamActivity
import org.rfcx.audiomoth.view.configure.ConfigureActivity
import org.rfcx.audiomoth.view.configure.ConfigureFragment.Companion.DASHBOARD_STREAM

class DashboardStreamActivity : AppCompatActivity() {
    private val dashboardStreamAdapter by lazy { DashboardStreamAdapter() }
    private var streams = ArrayList<String>()
    var currentDeviceId = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dashboard)

        if (intent.hasExtra(CreateStreamActivity.DEVICE_ID)) {
            val deviceId = intent.getStringExtra(CreateStreamActivity.DEVICE_ID)
            if (deviceId != null) {
                currentDeviceId = deviceId
                val docRef =
                    Firestore().db.collection(CreateStreamActivity.DEVICES).document(deviceId)
                docRef.collection("streams").get()
                    .addOnSuccessListener { document ->
                        if (document != null) {
                            val data = document.documents
                            data.forEach {
                                streams.add(it.id)
                            }
                            dashboardStreamAdapter.items = streams
                        } else {
                            Log.d(TAG, "No such document")
                        }
                    }
                    .addOnFailureListener { exception ->
                        Log.d(TAG, "get failed with ", exception)
                    }
            }
        }

        streamsRecyclerView.apply {
            val alertsLayoutManager = LinearLayoutManager(context)
            layoutManager = alertsLayoutManager
            adapter = dashboardStreamAdapter
        }

        dashboardStreamAdapter.onDashboardClick = object : OnDashboardClickListener {
            override fun onDashboardClick(streamName: String) {
                val docRef =
                    Firestore().db.collection(CreateStreamActivity.DEVICES)
                        .document(currentDeviceId)
                docRef.collection("streams").document(streamName).get()
                    .addOnSuccessListener { document ->
                        if (document != null) {
                            val data = document.data
                            if (data != null) {
                                val stream = Stream(
                                    data["streamName"].toString(),
                                    data["gain"].toString().toInt(),
                                    data["sampleRateKiloHertz"].toString().toInt(),
                                    data["customRecordingPeriod"] as Boolean,
                                    data["recordingDurationSecond"].toString().toInt(),
                                    data["sleepDurationSecond"].toString().toInt(),
                                    data["recordingPeriodList"] as ArrayList<String>,
                                    data["durationSelected"].toString()
                                )
                                ConfigureActivity.startActivity(
                                    this@DashboardStreamActivity,
                                    currentDeviceId,
                                    streamName,
                                    stream,
                                    DASHBOARD_STREAM
                                )
                            }
                        } else {
                            Log.d(TAG, "No such document")
                        }
                    }
                    .addOnFailureListener { exception ->
                        Log.d(TAG, "get failed with ", exception)
                    }
            }
        }
    }

    companion object {

        const val TAG = "DashboardStreamActivity"
        fun startActivity(context: Context, deviceId: String) {
            val intent = Intent(context, DashboardStreamActivity::class.java)
            intent.putExtra(CreateStreamActivity.DEVICE_ID, deviceId)
            context.startActivity(intent)
        }
    }
}

interface OnDashboardClickListener {
    fun onDashboardClick(streamName: String)
}