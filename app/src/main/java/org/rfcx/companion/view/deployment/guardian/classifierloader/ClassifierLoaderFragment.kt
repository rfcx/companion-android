package org.rfcx.companion.view.deployment.guardian.classifierloader

import android.content.Context
import android.os.Bundle
import android.os.CountDownTimer
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.android.synthetic.main.fragment_software_update.*
import org.rfcx.companion.R
import org.rfcx.companion.connection.socket.FileSocketManager
import org.rfcx.companion.entity.Software
import org.rfcx.companion.entity.socket.Classifier
import org.rfcx.companion.util.file.ClassifierUtils
import org.rfcx.companion.view.deployment.guardian.GuardianDeploymentProtocol
import org.rfcx.companion.view.deployment.guardian.softwareupdate.SoftwareUpdateAdapter
import java.util.*

class ClassifierLoaderFragment : Fragment(), ClassifierLoadListener {
    private var deploymentProtocol: GuardianDeploymentProtocol? = null
    private var loadingTimer: CountDownTimer? = null
    private var classifierAdapter: ClassifierAdapter? = null

    override fun onAttach(context: Context) {
        super.onAttach(context)
        deploymentProtocol = (context as GuardianDeploymentProtocol)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_software_update, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        deploymentProtocol?.let {
            it.setToolbarSubtitle()
            it.setMenuToolbar(false)
            it.showToolbar()
            it.setToolbarTitle()
        }

        nextButton.setOnClickListener {
            deploymentProtocol?.nextStep()
        }

        val classifiers = ClassifierUtils.getAllDownloadedClassifiers(requireContext())
        if (classifiers.isNullOrEmpty()) {
            noSoftwareText.visibility = View.VISIBLE
        } else {
            populateAdapterWithInfo(classifiers)
        }
    }

    private fun populateAdapterWithInfo(classifiers: List<Classifier>) {
        classifierAdapter = ClassifierAdapter(this)
        classifierAdapter?.let {
            val layoutManager = LinearLayoutManager(context)
            apkRecyclerView.layoutManager = layoutManager
            apkRecyclerView.adapter = it
            apkRecyclerView.addItemDecoration(
                DividerItemDecoration(
                    this.context,
                    DividerItemDecoration.VERTICAL
                )
            )
            it.items = classifiers
            it.notifyDataSetChanged()
        }
    }

    private fun startTimer() {
        loadingTimer = object: CountDownTimer(60000, 1000) {
            override fun onTick(millisUntilFinished: Long) { }

            override fun onFinish() {
                classifierAdapter?.let {
                    if (it.getLoading()) {
                        it.hideLoading()
                    }
                }
                stopTimer()
            }
        }
        loadingTimer?.start()
    }

    private fun stopTimer() {
        loadingTimer?.cancel()
        loadingTimer = null
    }

    companion object {
        @JvmStatic
        fun newInstance() = ClassifierLoaderFragment()
    }

    override fun onLoadClicked(path: String) {
        FileSocketManager.sendFile(path)
        nextButton.isEnabled = false
        startTimer()
    }
}
