package org.rfcx.companion.view.detail

import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import kotlinx.android.synthetic.main.activity_display_image.*
import kotlinx.android.synthetic.main.toolbar_default.*
import org.rfcx.companion.R
import org.rfcx.companion.extension.setDeploymentImage

class DisplayImageActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_display_image)
        setupToolbar()

        val path = intent.extras?.getString(PATH_IMAGE) ?: ""
        imageView.setDeploymentImage(url = path, blur = false)
    }

    private fun setupToolbar() {
        setSupportActionBar(toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowHomeEnabled(true)
            title = "Image"
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }

    companion object {
        const val PATH_IMAGE = "PATH_IMAGE"

        fun startActivity(context: Context, path: String) {
            val intent = Intent(context, DisplayImageActivity::class.java)
            intent.putExtra(PATH_IMAGE, path)
            context.startActivity(intent)
        }
    }
}
