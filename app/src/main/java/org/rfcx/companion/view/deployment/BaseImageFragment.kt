package org.rfcx.companion.view.deployment

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import com.opensooq.supernova.gligar.GligarPicker
import kotlinx.android.synthetic.main.fragment_deploy.*
import org.rfcx.companion.util.CameraPermissions
import org.rfcx.companion.util.GalleryPermissions
import org.rfcx.companion.util.ImageUtils
import org.rfcx.companion.view.deployment.songmeter.SongMeterDeploymentProtocol
import org.rfcx.companion.view.detail.DisplayImageActivity
import org.rfcx.companion.view.dialog.GuidelineButtonClickListener

abstract class BaseImageFragment : Fragment(), ImageClickListener, GuidelineButtonClickListener {

    private var imageAdapter: ImageAdapter? = null
    private var filePath: String? = null

    private val cameraPermissions by lazy { CameraPermissions(context as Activity) }
    private val galleryPermissions by lazy { GalleryPermissions(context as Activity) }

    private val CAMERA_IMAGE_PATH = "cameraImagePath"

    var audioMothDeploymentProtocol: BaseDeploymentProtocol? = null
    var songMeterDeploymentProtocol: BaseDeploymentProtocol? = null

    override fun onAttach(context: Context) {
        super.onAttach(context)
        when (context) {
            is AudioMothDeploymentProtocol -> audioMothDeploymentProtocol = context
            is SongMeterDeploymentProtocol -> songMeterDeploymentProtocol = context
        }
    }

    fun getImageAdapter(): ImageAdapter {
        if (imageAdapter != null) {
            return imageAdapter!!
        }
        imageAdapter = ImageAdapter(this)
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
        handleGligarPickerResult(requestCode, resultCode, data)
    }

    private fun setupImages() {
        getImageAdapter().setPlaceHolders(listOf("device", "bush", "tree", "sky", "over view"))
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
        GligarPicker()
            .requestCode(ImageUtils.REQUEST_GALLERY)
            .limit(1)
            .withFragment(this)
            .show()
    }

    private fun handleGligarPickerResult(requestCode: Int, resultCode: Int, intentData: Intent?) {
        if (requestCode != ImageUtils.REQUEST_GALLERY || resultCode != Activity.RESULT_OK || intentData == null) return

        val results = intentData.extras?.getStringArray(GligarPicker.IMAGES_RESULT)
        results?.forEach {
            getImageAdapter().updateTakeOrChooseImage(it)
        }
    }
}
