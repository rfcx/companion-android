package org.rfcx.audiomoth.view.configure

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_connect_device.*
import org.rfcx.audiomoth.R
import org.rfcx.audiomoth.util.ConnectDevice

class ConnectDeviceActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_connect_device)

        configureButton.setOnClickListener {
            ConnectDevice().playSound(this)
        }
    }
}
