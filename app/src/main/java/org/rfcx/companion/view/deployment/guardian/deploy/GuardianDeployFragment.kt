package org.rfcx.companion.view.deployment.guardian.deploy

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.android.synthetic.main.fragment_guardian_deploy.*
import org.rfcx.companion.R
import org.rfcx.companion.entity.Screen
import org.rfcx.companion.util.Analytics
import org.rfcx.companion.view.deployment.BaseImageFragment
import org.rfcx.companion.view.deployment.guardian.GuardianDeploymentProtocol

class GuardianDeployFragment() : BaseImageFragment() {

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
            it.setToolbarSubtitle()
            it.setMenuToolbar(false)
            it.showToolbar()
            it.setToolbarTitle()
        }

        setupImageRecycler()

        addPhotoButton.setOnClickListener {
            openGligarPicker()
        }

        finishButton.setOnClickListener {
//            val images = getImageAdapter().getNewAttachImage()
//            if (images.isNotEmpty()) {
//                analytics?.trackAddDeploymentImageEvent(Device.AUDIOMOTH.value)
//            }
//            deploymentProtocol?.setImages(images)
//            deploymentProtocol?.nextStep()
        }

        val deployment = deploymentProtocol?.getImages()
        if (deployment != null && deployment.isNotEmpty()) {
            val pathList = mutableListOf<String>()
            deployment.forEach {
                pathList.add(it)
            }
//            getImageAdapter().addImages(pathList)
        }
    }

    private fun setupImageRecycler() {
        attachImageRecycler.apply {
            adapter = getImageAdapter()
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
            setHasFixedSize(true)
        }
//        getImageAdapter().setImages(arrayListOf())
    }

    override fun onResume() {
        super.onResume()
        analytics?.trackScreen(Screen.GUARDIAN_DEPLOY)
    }

    override fun onPlaceHolderClick() {
        TODO("Not yet implemented")
    }

    override fun onImageClick(path: String?) {
        TODO("Not yet implemented")
    }

    override fun onDeleteClick() {
        TODO("Not yet implemented")
    }

    override fun onContinueClick() {
        TODO("Not yet implemented")
    }

    companion object {
        fun newInstance(): GuardianDeployFragment {
            return GuardianDeployFragment()
        }
    }
}
