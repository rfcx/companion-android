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
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.android.synthetic.main.activity_create_stream.*
import org.rfcx.audiomoth.R
import java.util.ArrayList

class CreateStreamActivity : AppCompatActivity() {

    private val db = Firebase.firestore

    private var arrayAdapter: ArrayAdapter<String>? = null
    val sites: ArrayList<String>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create_stream)

        db.collection("sites")
            .get()
            .addOnSuccessListener { result ->
                for (document in result) {
//                    Log.d(TAG, "${document.id} => ${document.data}")
                    sites?.add(document.data["name"].toString())
                }
            }
            .addOnFailureListener { exception ->
                Log.w(TAG, "Error getting documents.", exception)
            }

        if (intent.hasExtra(DEVICE_ID)) {
            val deviceId = intent.getStringExtra(DEVICE_ID)
            if (deviceId != null) {
                deviceIdTextView.text = getString(R.string.device_id_number, deviceId)
            }
        }

        arrayAdapter =
            sites?.let { ArrayAdapter(this, R.layout.support_simple_spinner_dropdown_item, it) }
        siteSpinner.adapter = arrayAdapter

        //spinner as dialog
        siteSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                Toast.makeText(this@CreateStreamActivity, sites?.get(position), Toast.LENGTH_SHORT)
                    .show()
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
            }
        }

        streamNameEditText.showKeyboard()
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
