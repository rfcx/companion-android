package org.rfcx.companion.view.deployment

import android.app.Activity
import android.content.Intent
import android.content.pm.ActivityInfo
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import com.zhihu.matisse.Matisse
import com.zhihu.matisse.MimeType
import java.io.File
import org.rfcx.companion.R
import org.rfcx.companion.util.*

abstract class BaseImageFragment : Fragment() {

    protected abstract fun didAddImages(imagePaths: List<String>)
    protected abstract fun didRemoveImage(imagePath: String)

    protected val imageAdapter by lazy { ImageAdapter() }
    private var imageFile: File? = null

    private val cameraPermissions by lazy { CameraPermissions(context as Activity) }
    private val galleryPermissions by lazy { GalleryPermissions(context as Activity) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setupImages()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        handleTakePhotoResult(requestCode, resultCode)
        handleGalleryResult(requestCode, resultCode, data)
    }

    private fun setupImages() {
        imageAdapter.onImageAdapterClickListener =
            object :
                OnImageAdapterClickListener {

                override fun onDeleteImageClick(position: Int, imagePath: String) {
                    imageAdapter.removeAt(position)
                    didRemoveImage(imagePath)
                }
            }

        imageAdapter.setImages(arrayListOf())
    }

    fun takePhoto() {
        if (!cameraPermissions.allowed()) {
            imageFile = null
            cameraPermissions.check { }
        } else {
            startTakePhoto()
        }
    }

    private fun startTakePhoto() {
        val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        imageFile = ImageUtils.createImageFile()
        if (imageFile != null) {
            val photoURI =
                context?.let {
                    FileProvider.getUriForFile(
                        it,
                        ImageUtils.FILE_CONTENT_PROVIDER,
                        imageFile!!
                    )
                }
            takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI)
            startActivityForResult(takePictureIntent, ImageUtils.REQUEST_TAKE_PHOTO)
        } else {
            Toast.makeText(context, R.string.can_not_create_image, Toast.LENGTH_SHORT).show()
        }
    }

    private fun handleTakePhotoResult(requestCode: Int, resultCode: Int) {
        if (requestCode != ImageUtils.REQUEST_TAKE_PHOTO) return

        if (resultCode == Activity.RESULT_OK) {
            imageFile?.let {
                val pathList = listOf(it.absolutePath)
                imageAdapter.addImages(pathList)
                didAddImages(pathList)
            }
        } else {
            // remove file image
            imageFile?.let {
                ImageFileUtils.removeFile(it)
                this.imageFile = null
            }
        }
    }

    fun openGallery() {
        if (!galleryPermissions.allowed()) {
            imageFile = null
            galleryPermissions.check { }
        } else {
            startOpenGallery()
        }
    }

    private fun startOpenGallery() {
        val remainingImage = ImageAdapter.MAX_IMAGE_SIZE - imageAdapter.getImageCount()
        Matisse.from(this)
            .choose(MimeType.ofImage())
            .countable(true)
            .maxSelectable(remainingImage)
            .restrictOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT)
            .thumbnailScale(0.85f)
            .imageEngine(GlideV4ImageEngine())
            .theme(R.style.Matisse_Dracula)
            .forResult(ImageUtils.REQUEST_GALLERY)
    }

    private fun handleGalleryResult(requestCode: Int, resultCode: Int, intentData: Intent?) {
        if (requestCode != ImageUtils.REQUEST_GALLERY || resultCode != Activity.RESULT_OK || intentData == null) return

        val pathList = mutableListOf<String>()
        val results = Matisse.obtainResult(intentData)
        results.forEach {
            val imagePath = context?.let { it1 -> ImageFileUtils.findRealPath(it1, it) }
            imagePath?.let { path ->
                pathList.add(path)
            }
        }
        imageAdapter.addImages(pathList)
        didAddImages(pathList)
    }
}
