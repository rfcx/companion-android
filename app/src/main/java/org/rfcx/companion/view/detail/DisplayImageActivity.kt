package org.rfcx.companion.view.detail

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.veinhorn.scrollgalleryview.MediaInfo
import com.veinhorn.scrollgalleryview.ScrollGalleryView
import com.veinhorn.scrollgalleryview.loader.picasso.PicassoImageLoader
import kotlinx.android.synthetic.main.activity_display_image.*
import kotlinx.android.synthetic.main.toolbar_default.*
import org.rfcx.companion.R

class DisplayImageActivity : AppCompatActivity() {
    lateinit var galleryView: ScrollGalleryView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_display_image)
        setupToolbar()

        val paths = intent.extras?.getStringArrayList(PATH_IMAGE) ?: ArrayList()
        val infoImage: ArrayList<MediaInfo> = ArrayList(paths.size)
        for (url in paths) infoImage.add(MediaInfo.mediaLoader(PicassoImageLoader(url)))

        galleryView = scrollGalleryView
        galleryView
            .withHiddenThumbnails(true)
            .setThumbnailSize(100)
            .setFragmentManager(supportFragmentManager)
            .addMedia(infoImage)
            .setZoom(true)
    }

    private fun setupToolbar() {
        setSupportActionBar(toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowHomeEnabled(true)
            title = getString(R.string.image)
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
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
