package org.rfcx.companion.view.deployment

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import com.opensooq.supernova.gligar.GligarPicker
import kotlinx.android.synthetic.main.fragment_deploy.*
import org.rfcx.companion.util.CameraPermissions
import org.rfcx.companion.util.GalleryPermissions
import org.rfcx.companion.util.ImageFileUtils
import org.rfcx.companion.util.ImageUtils
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
        handleGligarPickerResult(requestCode, resultCode, data)
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

    fun openGligarPicker() {
        if (!cameraPermissions.allowed() || !galleryPermissions.allowed()) {
            filePath = null
            if (!cameraPermissions.allowed()) cameraPermissions.check { }
            if (!galleryPermissions.allowed()) galleryPermissions.check { }
        } else {
            startOpenGligarPicker()
        }
    }

    private fun startOpenGligarPicker() {
        val remainingImage = ImageAdapter.MAX_IMAGE_SIZE - getImageAdapter().getImageCount()
        GligarPicker()
            .requestCode(ImageUtils.REQUEST_GALLERY)
            .limit(remainingImage)
            .withFragment(this)
            .show()
    }

    private fun handleGligarPickerResult(requestCode: Int, resultCode: Int, intentData: Intent?) {
        if (requestCode != ImageUtils.REQUEST_GALLERY || resultCode != Activity.RESULT_OK || intentData == null) return

        val pathList = mutableListOf<String>()
        val results = intentData.extras?.getStringArray(GligarPicker.IMAGES_RESULT)
        results?.forEach {
            pathList.add(it)
        }
        getImageAdapter().addImages(pathList)
        didAddImages(pathList)
        hideAddImagesButton()
    }

    private fun hideAddImagesButton() {
        addPhotoButton.isEnabled = getImageAdapter().getImageCount() < 5
    }
}
