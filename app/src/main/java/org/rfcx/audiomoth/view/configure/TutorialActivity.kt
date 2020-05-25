package org.rfcx.audiomoth.view.configure

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_tutorial.*
import org.rfcx.audiomoth.MainActivity
import org.rfcx.audiomoth.R

class TutorialActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_tutorial)

        skipTextView.setOnClickListener {
            MainActivity.startActivity(this, null)
            finish()
        }
    }

    companion object {
        fun startActivity(context: Context) {
            val intent = Intent(context, TutorialActivity::class.java)
            context.startActivity(intent)
        }
    }
}
