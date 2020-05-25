package org.rfcx.audiomoth.view.configure


import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.android.synthetic.main.fragment_deploy.*
import org.rfcx.audiomoth.R
import org.rfcx.audiomoth.util.Storage
import org.rfcx.audiomoth.view.DeploymentProtocol

class DeployFragment : BaseImageFragment() {

    private var deploymentProtocol: DeploymentProtocol? = null

    override fun onAttach(context: Context) {
        super.onAttach(context)
        deploymentProtocol = (context as DeploymentProtocol)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_deploy, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupImageRecycler()
        finishButton.setOnClickListener {
            context?.let { it1 -> Storage(it1).uploadImage(imageAdapter.getNewAttachImage()) }
            deploymentProtocol?.completeStep()
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
