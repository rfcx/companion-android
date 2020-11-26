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
import kotlinx.android.synthetic.main.fragment_deploy.takePhotosTextView
import kotlinx.android.synthetic.main.fragment_guardian_deploy.*
import org.rfcx.companion.R
import org.rfcx.companion.entity.Device
import org.rfcx.companion.util.Analytics

class DeployFragment : BaseImageFragment() {

    private var edgeDeploymentProtocol: EdgeDeploymentProtocol? = null
    private val analytics by lazy { context?.let { Analytics(it) } }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        edgeDeploymentProtocol = (context as EdgeDeploymentProtocol)
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

        edgeDeploymentProtocol?.let {
            it.showToolbar()
            it.setCurrentPage(requireContext().resources.getStringArray(R.array.edge_optional_checks)[0])
            it.setToolbarTitle()
        }

        setupImageRecycler()

        finishButton.setOnClickListener {
            val images = imageAdapter.getNewAttachImage()
            if(images.isNotEmpty()) {
                analytics?.trackAddDeploymentImageEvent(Device.EDGE.value)
            }
            edgeDeploymentProtocol?.setImages(images)
            edgeDeploymentProtocol?.nextStep()
        }

        val deployment = edgeDeploymentProtocol?.getImages()
        if (deployment != null && deployment.isNotEmpty()) {
            val pathList = mutableListOf<String>()
            deployment.forEach {
                pathList.add(it)
            }
            imageAdapter.addImages(pathList)
            didAddImages(pathList)
        }
    }

    private fun setupImageRecycler() {
        attachImageRecycler.apply {
            adapter = imageAdapter
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
            setHasFixedSize(true)
        }
        imageAdapter.setImages(arrayListOf())
    }

    override fun didAddImages(imagePaths: List<String>) {}

    override fun didRemoveImage(imagePath: String) {}

    companion object {
        fun newInstance(): DeployFragment {
            return DeployFragment()
        }
    }
}
