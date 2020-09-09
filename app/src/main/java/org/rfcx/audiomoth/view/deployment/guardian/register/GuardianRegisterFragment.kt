package org.rfcx.audiomoth.view.deployment.guardian.register

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import kotlinx.android.synthetic.main.fragment_guardian_register.*
import org.rfcx.audiomoth.R
import org.rfcx.audiomoth.view.deployment.guardian.GuardianDeploymentProtocol

class GuardianRegisterFragment : Fragment() {

    private var deploymentProtocol: GuardianDeploymentProtocol? = null

    override fun onAttach(context: Context) {
        super.onAttach(context)
        deploymentProtocol = (context as GuardianDeploymentProtocol)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_guardian_register, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        deploymentProtocol?.hideCompleteButton()

        registerFinishButton.setOnClickListener {
            deploymentProtocol?.nextStep()
        }
    }

    companion object {
        fun newInstance(): GuardianRegisterFragment {
            return GuardianRegisterFragment()
        }
    }

}
