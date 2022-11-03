package org.rfcx.companion.view.deployment

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import org.rfcx.companion.R

class ChoosePhotosFragment : Fragment() {

    private var deploymentProtocol: BaseDeploymentProtocol? = null

    override fun onAttach(context: Context) {
        super.onAttach(context)
        deploymentProtocol = context as BaseDeploymentProtocol
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_choose_photos, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

    }

    companion object {
        fun newInstance(): ChoosePhotosFragment {
            return ChoosePhotosFragment()
        }
    }
}
