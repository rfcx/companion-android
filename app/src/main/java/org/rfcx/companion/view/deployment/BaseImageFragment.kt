package org.rfcx.companion.view.deployment

import android.app.Activity
import android.content.Intent
import android.content.pm.ActivityInfo
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import com.zhihu.matisse.Matisse
import com.zhihu.matisse.MimeType
import kotlinx.android.synthetic.main.fragment_deploy.*
import org.rfcx.companion.R
import org.rfcx.companion.util.*
import org.rfcx.companion.view.detail.DisplayImageActivity
import java.io.File

abstract class BaseImageFragment : Fragment() {

    protected abstract fun didAddImages(imagePaths: List<String>)
    protected abstract fun didRemoveImage(imagePath: String)

    private var imageAdapter: ImageAdapter? = null
    private var filePath: String? = null

    private val cameraPermissions by lazy { CameraPermissions(context as Activity) }
    private val galleryPermissions by lazy { GalleryPermissions(context as Activity) }

    private val CAMERA_IMAGE_PATH = "cameraImagePath"

    fun getImageAdapter(): ImageAdapter {
        if (imageAdapter != null) {
            return imageAdapter!!
        }
        imageAdapter = ImageAdapter()
        return imageAdapter!!
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setupImages()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        if (filePath != null) {
            outState.putString(CAMERA_IMAGE_PATH, filePath.toString())
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        savedInstanceState?.let {
            if (it.containsKey(CAMERA_IMAGE_PATH)) {
                filePath = it.getString(CAMERA_IMAGE_PATH)
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        handleTakePhotoResult(requestCode, resultCode)
        handleGalleryResult(requestCode, resultCode, data)
    }

    private fun setupImages() {
        getImageAdapter().onImageAdapterClickListener =
            object :
                OnImageAdapterClickListener {

                override fun onDeleteImageClick(position: Int, imagePath: String) {
                    getImageAdapter().removeAt(position)
                    didRemoveImage(imagePath)
                    hideAddImagesButton()
                }

                override fun onImageClick(imagePath: String) {
                    val list = arrayListOf<String>()
                    getImageAdapter().getNewAttachImage().forEach { list.add("file://$it") }
                    val index = list.indexOf("file://$imagePath")
                    list.removeAt(index)
                    list.add(0, "file://$imagePath")

                    context?.let { DisplayImageActivity.startActivity(it, list) }
                }
            }

        getImageAdapter().setImages(arrayListOf())
    }

    fun takePhoto() {
        if (!cameraPermissions.allowed()) {
            filePath = null
            cameraPermissions.check { }
        } else {
            startTakePhoto()
        }
    }

    private fun startTakePhoto() {
        val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        val imageFile = ImageUtils.createImageFile()
        filePath = imageFile.absolutePath
        val imageUri =
            context?.let {
                FileProvider.getUriForFile(
                    it,
                    ImageUtils.FILE_CONTENT_PROVIDER,
                    imageFile
                )
            }
        takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri)
        startActivityForResult(takePictureIntent, ImageUtils.REQUEST_TAKE_PHOTO)
    }

    private fun handleTakePhotoResult(requestCode: Int, resultCode: Int) {
        if (requestCode != ImageUtils.REQUEST_TAKE_PHOTO) return

        if (resultCode == Activity.RESULT_OK) {
            filePath?.let {
                val pathList = listOf(it)
                getImageAdapter().addImages(pathList)
                didAddImages(pathList)
                hideAddImagesButton()
            }
        } else {
            // remove file image
            filePath?.let {
                ImageFileUtils.removeFile(File(it))
                this.filePath = null
            }
        }
    }

    fun openGallery() {
        if (!galleryPermissions.allowed()) {
            filePath = null
            galleryPermissions.check { }
        } else {
            startOpenGallery()
        }
    }

    private fun startOpenGallery() {
        val remainingImage = ImageAdapter.MAX_IMAGE_SIZE - getImageAdapter().getImageCount()
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
        getImageAdapter().addImages(pathList)
        didAddImages(pathList)
        hideAddImagesButton()
    }

    private fun hideAddImagesButton() {
        takePhotoButton.isEnabled = getImageAdapter().getImageCount() < 5
        openGalleryButton.isEnabled = getImageAdapter().getImageCount() < 5
    }
}
