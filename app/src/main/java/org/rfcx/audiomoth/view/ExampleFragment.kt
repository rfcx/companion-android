package org.rfcx.audiomoth.view

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import kotlinx.android.synthetic.main.fragment_example.*
import org.rfcx.audiomoth.R

//TODO: DELETE EXAMPLE FRAGMENT
class ExampleFragment : Fragment() {
    private var deploymentProtocol: DeploymentProtocol? = null
    private var stepNumber: Int? = null

    override fun onAttach(context: Context) {
        super.onAttach(context)
        deploymentProtocol = context as DeploymentProtocol
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            stepNumber = it.getInt(ARG_STEP_NUMBER)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_example, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        textView.text = String.format("Current Step: %d", stepNumber)
        subTextView.text = String.format("Next Step is %s", deploymentProtocol?.getNameNextStep())
    }

    companion object {
        private const val ARG_STEP_NUMBER = "ARG_STEP_NUMBER"

        @JvmStatic
        fun newInstance(index: Int) =
            ExampleFragment().apply {
                arguments = Bundle().apply {
                    putInt(ARG_STEP_NUMBER, index + 1)
                }
            }
    }
}
