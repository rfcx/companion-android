package org.rfcx.companion.view.detail

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.veinhorn.scrollgalleryview.MediaInfo
import com.veinhorn.scrollgalleryview.ScrollGalleryView
import kotlinx.android.synthetic.main.activity_display_image.*
import ogbe.ozioma.com.glideimageloader.GlideImageLoader
import org.rfcx.companion.R
import org.rfcx.companion.extension.toGlideWithHeader
import org.rfcx.companion.util.getIdToken

class DisplayImageActivity : AppCompatActivity() {
    lateinit var galleryView: ScrollGalleryView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_display_image)

        val paths = intent.extras?.getStringArrayList(PATH_IMAGE) ?: ArrayList()
        val infoImage: ArrayList<MediaInfo> = ArrayList(paths.size)
        for (url in paths) {
            Log.d("image", url.toGlideWithHeader(this.getIdToken()!!))
            infoImage.add(MediaInfo.mediaLoader(GlideImageLoader(url.toGlideWithHeader(this.getIdToken()!!))))
        }

        galleryView = scrollGalleryView
        galleryView
            .withHiddenThumbnails(true)
            .setThumbnailSize(100)
            .setFragmentManager(supportFragmentManager)
            .addMedia(infoImage)
            .setZoom(true)
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
