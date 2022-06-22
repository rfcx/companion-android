package org.rfcx.companion.view.deployment.guardian.classifier

import android.content.Context
import android.os.Bundle
import android.os.CountDownTimer
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import io.realm.Realm
import kotlinx.android.synthetic.main.fragment_classifier.*
import kotlinx.android.synthetic.main.fragment_software_update.nextButton
import org.rfcx.companion.R
import org.rfcx.companion.connection.socket.FileSocketManager
import org.rfcx.companion.connection.socket.GuardianSocketManager
import org.rfcx.companion.entity.guardian.Classifier
import org.rfcx.companion.localdb.ClassifierDb
import org.rfcx.companion.util.RealmHelper
import org.rfcx.companion.util.file.ClassifierUtils
import org.rfcx.companion.view.deployment.guardian.GuardianDeploymentProtocol

class ClassifierLoadFragment : Fragment(), ChildrenClickedListener {
    private var deploymentProtocol: GuardianDeploymentProtocol? = null
    var classifierLoadAdapter: ClassifierLoadAdapter? = null
    private var selectedFile: ClassifierItem.ClassifierVersion? = null
    private var loadingTimer: CountDownTimer? = null

    override fun onAttach(context: Context) {
        super.onAttach(context)
        deploymentProtocol = (context as GuardianDeploymentProtocol)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_classifier, container, false)
    }

    private fun populateAdapterWithInfo(classifiers: List<Classifier>) {
        classifierLoadAdapter = ClassifierLoadAdapter(
            this,
            classifiers
        )
        classifierLoadAdapter?.let {
            val layoutManager = LinearLayoutManager(context)
            classifierRecyclerView.layoutManager = layoutManager
            classifierRecyclerView.adapter = it
            classifierRecyclerView.addItemDecoration(
                DividerItemDecoration(
                    this.context,
                    DividerItemDecoration.VERTICAL
                )
            )
            it.notifyDataSetChanged()
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        deploymentProtocol?.let {
            it.setToolbarSubtitle()
            it.setMenuToolbar(false)
            it.showToolbar()
            it.setToolbarTitle()
        }

        GuardianSocketManager.pingBlob.observe(
            viewLifecycleOwner
        ) {
            requireActivity().runOnUiThread {
                deploymentProtocol?.getSoftwareVersion()?.let {
                    classifierLoadAdapter?.classifierVersion = it
                    classifierLoadAdapter?.notifyDataSetChanged()

                    selectedFile?.let { selected ->
                        val selectedVersion = selected.classifier.version
                        val installedVersion = it[selected.classifier.version]
                        if (installedVersion != null && installedVersion == selectedVersion) {
                            classifierLoadAdapter?.hideLoading()
                            nextButton.isEnabled = true

                            stopTimer()
                        }
                    }
                }
            }
        }

        nextButton.setOnClickListener {
            deploymentProtocol?.nextStep()
        }

        val db = ClassifierDb(Realm.getInstance(RealmHelper.migrationConfig()))
        val classifiers = db.getAll()
        if (classifiers.isNullOrEmpty()) {
            noClassifierText.visibility = View.VISIBLE
        } else {
            populateAdapterWithInfo(classifiers)
        }
    }

    private fun startTimer() {
        loadingTimer = object : CountDownTimer(120000, 1000) {
            override fun onTick(millisUntilFinished: Long) { }

            override fun onFinish() {
                classifierLoadAdapter?.let {
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
        fun newInstance() = ClassifierLoadFragment()
    }

    override fun onItemClick(selectedClassifier: ClassifierItem.ClassifierVersion) {
        selectedFile = selectedClassifier
        FileSocketManager.sendFile(selectedClassifier.classifier)
        nextButton.isEnabled = false
        startTimer()
    }
}
