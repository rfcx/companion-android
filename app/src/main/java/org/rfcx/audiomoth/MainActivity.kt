package org.rfcx.audiomoth

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Paint.UNDERLINE_TEXT_FLAG
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.zxing.integration.android.IntentIntegrator
import kotlinx.android.synthetic.main.activity_main.*

open class MainActivity : AppCompatActivity() {

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        enterCodeTextView.paintFlags = enterCodeTextView.paintFlags or UNDERLINE_TEXT_FLAG

        scanQRButton.setOnClickListener {
            IntentIntegrator(this)
                .setPrompt("Scan a QR code")
                .setBeepEnabled(false)
                .initiateScan()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        val result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data)

        if (result != null) {
            if (result.contents == null) {
                Toast.makeText(this, "The data is empty", Toast.LENGTH_SHORT).show()
            } else {
                valueTextView.text = result.contents
            }

        } else {
            // the camera will not close if the result is still null
            super.onActivityResult(requestCode, resultCode, data)
        }
    }
}
