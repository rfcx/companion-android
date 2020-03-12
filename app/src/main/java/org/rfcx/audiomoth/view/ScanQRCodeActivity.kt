package org.rfcx.audiomoth.view

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import org.rfcx.audiomoth.R

class ScanQRCodeActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_scan_qrcode)
    }

    companion object {
        fun startActivity(context: Context) {
            val intent = Intent(context, ScanQRCodeActivity::class.java)
            context.startActivity(intent)
        }
    }
}
