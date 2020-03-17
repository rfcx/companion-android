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
import java.sql.Timestamp
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
        getSites()
        setSiteSpinner()
        addTextChanged()

        streamNameEditText.showKeyboard()

        createStreamButton.setOnClickListener {
            saveStreamData()
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
        Firestore().db.collection("sites")
            .get()
            .addOnSuccessListener { result ->
                sites = ArrayList()
                sitesId = ArrayList()
                result.map { sites.add(it.data["name"].toString()) }
                result.map { sitesId.add(it.id) }

                arrayAdapter.addAll(sites)
                arrayAdapter.notifyDataSetChanged()
            }
            .addOnFailureListener { exception ->
                Log.w(TAG, "Error getting documents.", exception)
            }
    }

    private fun View.showKeyboard() = this.let {
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0)
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

    fun saveStreamData() {
        if (intent.hasExtra(DEVICE_ID)) {
            val deviceId = intent.getStringExtra(DEVICE_ID)

            if (deviceId != null && site.isNotEmpty() && siteId.isNotEmpty() && nameStream.isNotEmpty()) {

                try {
                    val items = HashMap<String, Any>()
                    items["createdAt"] = Timestamp(System.currentTimeMillis()).toString()
                    items["isLogin"] = false
                    items["streamName"] = nameStream
                    items["siteName"] = site
                    items["siteId"] = siteId

                    Firestore().db.collection("device").document(deviceId).set(items)
                        .addOnSuccessListener { void: Void? ->
                            Toast.makeText(
                                this,
                                "Successfully uploaded to the database :)",
                                Toast.LENGTH_LONG
                            ).show()
                        }.addOnFailureListener { exception: java.lang.Exception ->
                            Toast.makeText(this, exception.toString(), Toast.LENGTH_LONG).show()
                        }
                } catch (e: Exception) {
                    Toast.makeText(this, e.toString(), Toast.LENGTH_LONG).show()
                }
            } else {
                Toast.makeText(this, "Please fill up the fields :(", Toast.LENGTH_LONG).show()
            }
        }
    }

    companion object {
        private const val TAG = "CreateStreamActivity"
        private const val DEVICE_ID = "DEVICE_ID"

        fun startActivity(context: Context, id: String?) {
            val intent = Intent(context, CreateStreamActivity::class.java)
            if (id != null)
                intent.putExtra(DEVICE_ID, id)
            context.startActivity(intent)
        }
    }
}
