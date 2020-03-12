package org.rfcx.audiomoth

import android.annotation.SuppressLint
import android.graphics.Paint.UNDERLINE_TEXT_FLAG
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_main.*
import org.rfcx.audiomoth.view.ScanQRCodeActivity

class MainActivity : AppCompatActivity() {

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        enterCodeTextView.paintFlags = enterCodeTextView.paintFlags or UNDERLINE_TEXT_FLAG

        scanQRButton.setOnClickListener {
            ScanQRCodeActivity.startActivity(this)
        }
    }
}
