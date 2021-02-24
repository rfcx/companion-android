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

        val paths = intent.extras?.getStringArrayList(PATH_IMAGE) ?: arrayListOf()
        val adapter = DisplayImageAdapter(paths, this)
        imageViewPager.orientation = ViewPager2.ORIENTATION_VERTICAL
        imageViewPager.adapter = adapter
    }

    companion object {
        const val PATH_IMAGE = "PATH_IMAGE"

        fun startActivity(context: Context, paths: ArrayList<String>) {
            val intent = Intent(context, DisplayImageActivity::class.java)
            intent.putStringArrayListExtra(PATH_IMAGE, paths)
            context.startActivity(intent)
        }
    }
}
