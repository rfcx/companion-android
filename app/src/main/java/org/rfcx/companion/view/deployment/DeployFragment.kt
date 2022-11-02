package org.rfcx.companion.view.deployment

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.android.synthetic.main.fragment_deploy.*
import org.rfcx.companion.R
import org.rfcx.companion.entity.Device
import org.rfcx.companion.entity.Screen
import org.rfcx.companion.util.Analytics
import org.rfcx.companion.view.deployment.songmeter.SongMeterDeploymentProtocol

class DeployFragment : BaseImageFragment() {

    private val analytics by lazy { context?.let { Analytics(it) } }
    private var screen: String? = null

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

        audioMothDeploymentProtocol?.let {
            it.showToolbar()
            it.setCurrentPage(requireContext().resources.getStringArray(R.array.edge_optional_checks)[0])
            it.setToolbarTitle()
            takePhotosTextView.text = getString(R.string.take_photos_audiomoth)
        }

        songMeterDeploymentProtocol?.let {
            it.showToolbar()
            it.setCurrentPage(requireContext().resources.getStringArray(R.array.song_meter_optional_checks)[0])
            it.setToolbarTitle()
            takePhotosTextView.text = getString(R.string.take_photos_songmeter)
        }

        setupImageRecycler()

        addPhotoButton.setOnClickListener {
            openGligarPicker()
        }

        finishButton.setOnClickListener {
            val images = getImageAdapter().getNewAttachImage()
            if (screen == Screen.AUDIO_MOTH_CHECK_LIST.id) {
                if (images.isNotEmpty()) {
                    analytics?.trackAddDeploymentImageEvent(Device.AUDIOMOTH.value)
                }
                audioMothDeploymentProtocol?.setImages(images)
                audioMothDeploymentProtocol?.nextStep()
            } else if (screen == Screen.SONG_METER_CHECK_LIST.id) {
                if (images.isNotEmpty()) {
                    analytics?.trackAddDeploymentImageEvent(Device.SONGMETER.value)
                }
                songMeterDeploymentProtocol?.setImages(images)
                songMeterDeploymentProtocol?.nextStep()
            }
        }

        val deployment = audioMothDeploymentProtocol?.getImages()
        if (deployment != null && deployment.isNotEmpty()) {
            val pathList = mutableListOf<String>()
            deployment.forEach {
                pathList.add(it)
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
