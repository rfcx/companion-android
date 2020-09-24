package org.rfcx.audiomoth.view.deployment

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.android.synthetic.main.fragment_deploy.*
import org.rfcx.audiomoth.R

class DeployFragment : BaseImageFragment() {

    private var edgeDeploymentProtocol: EdgeDeploymentProtocol? = null

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
            it.setToolbarTitle()
        }

        setupImageRecycler()
        finishButton.setOnClickListener {
            val images = imageAdapter.getNewAttachImage()
            edgeDeploymentProtocol?.setImages(images)
            edgeDeploymentProtocol?.nextStep()
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
