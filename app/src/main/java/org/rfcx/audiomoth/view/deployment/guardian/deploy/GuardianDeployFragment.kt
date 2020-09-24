package org.rfcx.audiomoth.view.deployment.guardian.deploy

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.android.synthetic.main.fragment_deploy.*
import org.rfcx.audiomoth.R
import org.rfcx.audiomoth.entity.Screen
import org.rfcx.audiomoth.util.Analytics
import org.rfcx.audiomoth.view.deployment.BaseImageFragment
import org.rfcx.audiomoth.view.deployment.guardian.GuardianDeploymentProtocol

class GuardianDeployFragment : BaseImageFragment() {

    private var deploymentProtocol: GuardianDeploymentProtocol? = null
    private val analytics by lazy { context?.let { Analytics(it) } }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        deploymentProtocol = (context as GuardianDeploymentProtocol)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_guardian_deploy, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        deploymentProtocol?.let {
            it.showToolbar()
            it.setToolbarTitle()
        }

        setupImageRecycler()

        finishButton.setOnClickListener {
            val images = imageAdapter.getNewAttachImage()
            deploymentProtocol?.setImages(images)
            deploymentProtocol?.nextStep()
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

    override fun onResume() {
        super.onResume()
        analytics?.trackScreen(Screen.GUARDIAN_DEPLOY)
    }

    companion object {
        fun newInstance(): GuardianDeployFragment {
            return GuardianDeployFragment()
        }
    }
}
