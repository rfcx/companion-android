package org.rfcx.companion.view.detail

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.viewpager2.widget.ViewPager2
import kotlinx.android.synthetic.main.activity_display_image.*
import org.rfcx.companion.R

class DisplayImageActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_display_image)

        val paths = intent.extras?.getStringArray(PATH_IMAGE) ?: arrayOf()
        val adapter = DisplayImageAdapter(paths.toList(), this)
        imageViewPager.orientation = ViewPager2.ORIENTATION_HORIZONTAL
        imageViewPager.adapter = adapter
    }

    companion object {
        const val PATH_IMAGE = "PATH_IMAGE"

        fun startActivity(context: Context, paths: Array<String>) {
            val intent = Intent(context, DisplayImageActivity::class.java)
            intent.putExtra(PATH_IMAGE, paths)
            context.startActivity(intent)
        }
    }
}
