package org.rfcx.companion.view.deployment

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.android.synthetic.main.fragment_deploy.*
import kotlinx.android.synthetic.main.fragment_deploy.attachImageRecycler
import kotlinx.android.synthetic.main.fragment_deploy.finishButton
import org.rfcx.companion.R
import org.rfcx.companion.entity.Device
import org.rfcx.companion.util.Analytics

class DeployFragment : BaseImageFragment() {

    private var audioMothDeploymentProtocol: AudioMothDeploymentProtocol? = null
    private val analytics by lazy { context?.let { Analytics(it) } }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        audioMothDeploymentProtocol = (context as AudioMothDeploymentProtocol)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_deploy, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        audioMothDeploymentProtocol?.let {
            it.showToolbar()
            it.setCurrentPage(requireContext().resources.getStringArray(R.array.edge_optional_checks)[0])
            it.setToolbarTitle()
        }

        setupImageRecycler()

        takePhotoButton.setOnClickListener {
            takePhoto()
        }

        openGalleryButton.setOnClickListener {
            openGallery()
        }

        finishButton.setOnClickListener {
            val images = getImageAdapter().getNewAttachImage()
            if (images.isNotEmpty()) {
                analytics?.trackAddDeploymentImageEvent(Device.AUDIOMOTH.value)
            }
            audioMothDeploymentProtocol?.setImages(images)
            audioMothDeploymentProtocol?.nextStep()
        }

        val deployment = audioMothDeploymentProtocol?.getImages()
        if (deployment != null && deployment.isNotEmpty()) {
            val pathList = mutableListOf<String>()
            deployment.forEach {
                pathList.add(it)
            }
            getImageAdapter().addImages(pathList)
            didAddImages(pathList)
        }
    }

    private fun setupImageRecycler() {
        attachImageRecycler.apply {
            adapter = getImageAdapter()
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
            setHasFixedSize(true)
        }
        getImageAdapter().setImages(arrayListOf())
    }

    override fun didAddImages(imagePaths: List<String>) {}

    override fun didRemoveImage(imagePath: String) {}

    companion object {
        fun newInstance(): DeployFragment {
            return DeployFragment()
        }
    }
}
