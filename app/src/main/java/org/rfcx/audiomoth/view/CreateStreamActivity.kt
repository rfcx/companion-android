package org.rfcx.audiomoth.view

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_create_stream.*
import org.rfcx.audiomoth.R
import org.rfcx.audiomoth.util.Firestore
import org.rfcx.audiomoth.view.configure.ConfigureActivity
import org.rfcx.audiomoth.view.configure.ConfigureFragment.Companion.CREATE_STREAM
import java.util.*

class CreateStreamActivity : AppCompatActivity() {

    private lateinit var arrayAdapter: ArrayAdapter<String>
    var sites = ArrayList<String>()
    var sitesId = ArrayList<String>()

    var site = ""
    var siteId = ""
    var nameStream = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create_stream)

        setAdapter()
        setSiteSpinner()
        addTextChanged()

        //TODO: Check device id and get site after check
        getSites()
        checkDeviceId()

        createStreamButton.setOnClickListener {
            createStreamProgressBar.visibility = View.VISIBLE
            createStreamButton.isEnabled = false
            streamNameEditText.hideKeyboard()

            if (intent.hasExtra(DEVICE_ID)) {
                val deviceId = intent.getStringExtra(DEVICE_ID)
                if (deviceId != null) {
                    ConfigureActivity.startActivity(
                        this,
                        deviceId,
                        nameStream,
                        siteId,
                        site,
                        CREATE_STREAM
                    )
                    finish()
                }
            }
        }
    }

    private fun checkEdgeOrGuardian(deviceId: String) {
        // TODO: Change to do something after know is Edge or Guardian
        val firstChar = deviceId[0]
        if (firstChar == 'G') {
            Toast.makeText(this, "This is Guardian", Toast.LENGTH_SHORT).show()
        } else if (firstChar == 'E') {
            Toast.makeText(this, "This is Edge", Toast.LENGTH_SHORT).show()
        }
    }

    private fun checkDeviceId() {
        if (intent.hasExtra(DEVICE_ID)) {
            val deviceId = intent.getStringExtra(DEVICE_ID)
            if (deviceId != null) {
                checkEdgeOrGuardian(deviceId)
                // TODO: Check device id is exist in Firestore (if have will get site)
            }
        }
    }

    private fun setAdapter() {
        arrayAdapter = ArrayAdapter(this, R.layout.support_simple_spinner_dropdown_item, sites)
        siteSpinner.adapter = arrayAdapter
    }

    private fun setSiteSpinner() {
        siteSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                site = sites[position]
                siteId = sitesId[position]
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
            }
        }
    }

    private fun getSites() {
        Firestore().db.collection(SITES)
            .get()
            .addOnSuccessListener { result ->
                sites = ArrayList()
                sitesId = ArrayList()
                result.map { sites.add(it.data["name"].toString()) }
                result.map { sitesId.add(it.id) }

                site = sites[0]
                siteId = sitesId[0]

                arrayAdapter.addAll(sites)
                arrayAdapter.notifyDataSetChanged()
            }
            .addOnFailureListener { exception ->
                Log.w(TAG, "Error getting documents.", exception)
            }
    }

    private fun getSiteFromDeviceId(site: String, siteId: String) {
        sites = ArrayList()
        sitesId = ArrayList()

        sites.add(site)
        sitesId.add(siteId)

        arrayAdapter.addAll(sites)
        arrayAdapter.notifyDataSetChanged()
    }

    private fun View.hideKeyboard() = this.let {
        val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(windowToken, 0)
    }

    private fun addTextChanged() {
        streamNameEditText.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(p0: Editable?) {
                if (p0 != null) {
                    nameStream = p0.toString()

                    if (p0.isEmpty()) {
                        createStreamButton.isEnabled = false
                    }
                }
            }

            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
            }

            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
                createStreamButton.isEnabled = true
            }
        })
    }

    companion object {
        private const val TAG = "CreateStreamActivity"
        const val DEVICE_ID = "DEVICE_ID"
        const val DEVICES = "devices"
        private const val SITES = "sites"

        fun startActivity(context: Context, id: String?) {
            val intent = Intent(context, CreateStreamActivity::class.java)
            if (id != null)
                intent.putExtra(DEVICE_ID, id)
            context.startActivity(intent)
        }
    }
}
