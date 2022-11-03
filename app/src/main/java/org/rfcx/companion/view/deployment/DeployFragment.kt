package org.rfcx.companion.view.deployment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.recyclerview.widget.GridLayoutManager
import kotlinx.android.synthetic.main.fragment_deploy.*
import org.rfcx.companion.R
import org.rfcx.companion.util.Analytics
import org.rfcx.companion.view.detail.DisplayImageActivity
import org.rfcx.companion.view.dialog.PhotoGuidelineDialogFragment

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

    override fun onPlaceHolderClick() {
        val guidelineDialog: PhotoGuidelineDialogFragment =
            this.parentFragmentManager.findFragmentByTag("PhotoGuidelineDialogFragment") as PhotoGuidelineDialogFragment?
                ?: run {
                    PhotoGuidelineDialogFragment(this)
                }
        if (guidelineDialog.isVisible || guidelineDialog.isAdded) return
        guidelineDialog.show(this.parentFragmentManager, "PhotoGuidelineDialogFragment")
    }

    override fun onImageClick(path: String?) {
        if (path == null) return
        context?.let { DisplayImageActivity.startActivity(it, arrayOf("file://$path")) }
    }

    override fun onDeleteClick() {
        getImageAdapter().removeImage()
    }

    override fun onContinueClick() {
        openGligarPicker()
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
        }

        songMeterDeploymentProtocol?.let {
            it.showToolbar()
            it.setCurrentPage(requireContext().resources.getStringArray(R.array.song_meter_optional_checks)[0])
            it.setToolbarTitle()
        }

        setupImageRecycler()

        finishButton.setOnClickListener {
//            val images = getImageAdapter().getNewAttachImage()
//            if (screen == Screen.AUDIO_MOTH_CHECK_LIST.id) {
//                if (images.isNotEmpty()) {
//                    analytics?.trackAddDeploymentImageEvent(Device.AUDIOMOTH.value)
//                }
//                audioMothDeploymentProtocol?.setImages(images)
//                audioMothDeploymentProtocol?.nextStep()
//            } else if (screen == Screen.SONG_METER_CHECK_LIST.id) {
//                if (images.isNotEmpty()) {
//                    analytics?.trackAddDeploymentImageEvent(Device.SONGMETER.value)
//                }
//                songMeterDeploymentProtocol?.setImages(images)
//                songMeterDeploymentProtocol?.nextStep()
//            }
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
            layoutManager = GridLayoutManager(context, 3)
        }
//        getImageAdapter().setImages(arrayListOf())
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
