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
import org.rfcx.companion.entity.Screen
import org.rfcx.companion.util.Analytics
import org.rfcx.companion.view.deployment.songmeter.SongMeterDeploymentProtocol

class DeployFragment : BaseImageFragment() {

    private var edgeDeploymentProtocol: EdgeDeploymentProtocol? = null
    private var songMeterDeploymentProtocol: SongMeterDeploymentProtocol? = null
    private val analytics by lazy { context?.let { Analytics(it) } }
    private var screen: String? = null

    override fun onAttach(context: Context) {
        super.onAttach(context)
        try {
            edgeDeploymentProtocol = (context as EdgeDeploymentProtocol)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        try {
            songMeterDeploymentProtocol = (context as SongMeterDeploymentProtocol)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_deploy, container, false)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initIntent()
    }

    private fun initIntent() {
        arguments?.let {
            screen = it.getString(ARG_SCREEN)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        edgeDeploymentProtocol?.let {
            it.showToolbar()
            it.setCurrentPage(requireContext().resources.getStringArray(R.array.edge_optional_checks)[0])
            it.setToolbarTitle()
        }

        songMeterDeploymentProtocol?.let {
            it.showToolbar()
            it.setCurrentPage(requireContext().resources.getStringArray(R.array.song_meter_optional_checks)[0])
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

            if (screen == Screen.AUDIO_MOTH_CHECK_LIST.id) {
                if(images.isNotEmpty()) {
                    analytics?.trackAddDeploymentImageEvent(Device.AUDIOMOTH.value)
                }
                edgeDeploymentProtocol?.setImages(images)
                edgeDeploymentProtocol?.nextStep()
            } else {
                if(images.isNotEmpty()) {
                    analytics?.trackAddDeploymentImageEvent(Device.SONGMETER.value)
                }
                songMeterDeploymentProtocol?.setImages(images)
                songMeterDeploymentProtocol?.nextStep()
            }
        }

        if (screen == Screen.AUDIO_MOTH_CHECK_LIST.id) {
            val deployment = edgeDeploymentProtocol?.getImages()
            if (deployment != null && deployment.isNotEmpty()) {
                val pathList = mutableListOf<String>()
                deployment.forEach {
                    pathList.add(it)
                }
                getImageAdapter().addImages(pathList)
                didAddImages(pathList)
            }
        } else {
            val deployment = songMeterDeploymentProtocol?.getImages()
            if (deployment != null && deployment.isNotEmpty()) {
                val pathList = mutableListOf<String>()
                deployment.forEach {
                    pathList.add(it)
                }
                getImageAdapter().addImages(pathList)
                didAddImages(pathList)
            }
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
        private const val ARG_SCREEN = "screen"

        fun newInstance(screen: String): DeployFragment {
            return DeployFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_SCREEN, screen)
                }
            }
        }
    }
}
