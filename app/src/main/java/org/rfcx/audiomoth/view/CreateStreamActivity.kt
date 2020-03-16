package org.rfcx.audiomoth.view

import android.content.Context
import android.content.Intent
import android.os.Bundle
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
import java.util.*

class CreateStreamActivity : AppCompatActivity() {

    private lateinit var arrayAdapter: ArrayAdapter<String>
    var sites = ArrayList<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create_stream)

        if (intent.hasExtra(DEVICE_ID)) {
            val deviceId = intent.getStringExtra(DEVICE_ID)
            if (deviceId != null) {
                deviceIdTextView.text = getString(R.string.device_id_number, deviceId)
            }
        }

        setAdapter()
        getSites()
        setSiteSpinner()

        streamNameEditText.showKeyboard()
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
                Toast.makeText(this@CreateStreamActivity, sites[position], Toast.LENGTH_SHORT)
                    .show()
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
                result.map { sites.add(it.data["name"].toString()) }

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
