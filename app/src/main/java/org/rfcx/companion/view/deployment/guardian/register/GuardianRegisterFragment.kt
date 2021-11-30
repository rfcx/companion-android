package org.rfcx.companion.view.deployment.guardian.register

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import kotlinx.android.synthetic.main.fragment_guardian_register.*
import org.rfcx.companion.R
import org.rfcx.companion.connection.socket.GuardianSocketManager
import org.rfcx.companion.entity.Screen
import org.rfcx.companion.entity.request.GuardianRegisterRequest
import org.rfcx.companion.entity.response.GuardianRegisterResponse
import org.rfcx.companion.repo.ApiManager
import org.rfcx.companion.util.Analytics
import org.rfcx.companion.util.getIdToken
import org.rfcx.companion.view.deployment.guardian.GuardianDeploymentProtocol
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class GuardianRegisterFragment : Fragment() {

    private var deploymentProtocol: GuardianDeploymentProtocol? = null

    private val analytics by lazy { context?.let { Analytics(it) } }

    private var isWaitingRegistration = false

    override fun onAttach(context: Context) {
        super.onAttach(context)
        deploymentProtocol = (context as GuardianDeploymentProtocol)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_guardian_register, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        deploymentProtocol?.let {
            context?.getString(R.string.register_guardian)?.let { it1 -> it.setCurrentPage(it1) }
            it.setMenuToolbar(true)
            it.showToolbar()
            it.setToolbarTitle()
        }

        GuardianSocketManager.resetRegisterResult()
        isGuardianRegistered()

        registerGuardianButton.setOnClickListener {
            analytics?.trackRegisterGuardianEvent()
            registerGuardian()
        }

        registerFinishButton.setOnClickListener {
            analytics?.trackClickNextEvent(Screen.GUARDIAN_REGISTER.id)
            deploymentProtocol?.startCheckList()
        }
    }

    private fun setUIWaitingRegisterResponse() {
        registerGuardianButton.isEnabled = false
        registerResultTextView.text = requireContext().getString(R.string.registering)
    }

    private fun registerGuardian() {
        isWaitingRegistration = true
        setUIWaitingRegisterResponse()
        val guid = deploymentProtocol?.getGuid()
        val userToken = requireContext().getIdToken()
        if (guid != null && userToken != null) {
            ApiManager.getInstance().getDeviceApi2(getRadioValueForRegistration()).registerGuardian("Bearer $userToken", GuardianRegisterRequest(guid)).enqueue(
                object: Callback<GuardianRegisterResponse> {
                    override fun onResponse(
                        call: Call<GuardianRegisterResponse>,
                        response: Response<GuardianRegisterResponse>
                    ) {
                        val regResponse = response.body()
                        if (regResponse != null) {
                            GuardianSocketManager.sendGuardianRegistration(regResponse)
                        } else {
                            Toast.makeText(requireContext(), "Register failed: empty response", Toast.LENGTH_LONG).show()
                            resetUI()
                        }
                    }

                    override fun onFailure(call: Call<GuardianRegisterResponse>, t: Throwable) {
                        Toast.makeText(requireContext(), "Register failed: ${t.message}", Toast.LENGTH_LONG).show()
                        resetUI()
                    }
                }
            )
        } else {
            Toast.makeText(requireContext(), "Register failed: guid or token is null", Toast.LENGTH_LONG).show()
            resetUI()
        }
    }

    private fun isGuardianRegistered() {
        GuardianSocketManager.pingBlob.observe(viewLifecycleOwner, Observer {
            val isRegistered = deploymentProtocol?.isGuardianRegistered()
            if (isRegistered != null && isRegistered) {
                deploymentProtocol?.startCheckList()
                productionRadioButton.isEnabled = false
                stagingRadioButton.isEnabled = false
                registerGuardianButton.visibility = View.GONE
                registerResultTextView.text =
                    requireContext().getString(R.string.already_registered)
                registerFinishButton.visibility = View.VISIBLE
            } else {
                if (!isWaitingRegistration) {
                    resetUI()
                }
            }
        })
        registerResultTextView.text = requireContext().getString(R.string.check_registered)
        registerGuardianButton.isEnabled = false
    }

    private fun resetUI() {
        registerResultTextView.text = requireContext().getString(R.string.not_registered)
        registerGuardianButton.isEnabled = true
    }

    private fun getRadioValueForRegistration(): Boolean {
        return productionRadioButton.isChecked
    }

    override fun onResume() {
        super.onResume()
        analytics?.trackScreen(Screen.GUARDIAN_REGISTER)
    }

    companion object {
        fun newInstance(): GuardianRegisterFragment {
            return GuardianRegisterFragment()
        }
    }
}
