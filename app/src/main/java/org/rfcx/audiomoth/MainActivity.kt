package org.rfcx.audiomoth

import android.annotation.SuppressLint
import android.content.DialogInterface
import android.content.Intent
import android.graphics.Paint.UNDERLINE_TEXT_FLAG
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.zxing.integration.android.IntentIntegrator
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.alertlayout.view.*
import org.rfcx.audiomoth.view.CreateStreamActivity

open class MainActivity : AppCompatActivity() {

    @SuppressLint("SetTextI18n", "DefaultLocale", "ResourceType")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        enterCodeTextView.paintFlags = enterCodeTextView.paintFlags or UNDERLINE_TEXT_FLAG

        scanQRButton.setOnClickListener {
            val integrator = IntentIntegrator(this)
            integrator.setDesiredBarcodeFormats(IntentIntegrator.QR_CODE)
            integrator.setOrientationLocked(false)
            integrator.setPrompt(getString(R.string.scan_qr_code))
            integrator.setBeepEnabled(false)
            integrator.initiateScan()
        }

        enterCodeTextView.setOnClickListener {
            val view = layoutInflater.inflate(R.layout.alertlayout, null)
            val builder = AlertDialog.Builder(this)
            builder.setTitle(getString(R.string.enter_code).capitalize())
            builder.setIcon(R.drawable.ic_audiomoth)
            builder.setView(view)

            builder.setPositiveButton(getString(R.string.ok)) { dialog, _ ->
                dialog.dismiss()
                val deviceId = view.deviceIdEditText.text.toString().trim()
                CreateStreamActivity.startActivity(this, deviceId)
            }

            builder.setNeutralButton(getString(R.string.cancel)) { dialog, _ ->
                dialog.dismiss()
            }

            val alertDialog = builder.create()
            alertDialog.show()

            val buttonNeutral = alertDialog.getButton(DialogInterface.BUTTON_NEUTRAL)
            buttonNeutral.setTextColor(ContextCompat.getColor(this, R.color.text_secondary))
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        val result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data)

        if (result != null) {
            if (result.contents == null) {
                Toast.makeText(this, getText(R.string.code_empty), Toast.LENGTH_SHORT).show()
            } else {
                CreateStreamActivity.startActivity(this, result.contents)
            }

        } else {
            // the camera will not close if the result is still null
            super.onActivityResult(requestCode, resultCode, data)
        }
    }
}
