package org.rfcx.companion.view.deployment.guardian.classifier

import android.content.Context
import android.os.Bundle
import android.os.CountDownTimer
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.gson.Gson
import io.realm.Realm
import kotlinx.android.synthetic.main.fragment_classifier.*
import kotlinx.android.synthetic.main.fragment_software_update.nextButton
import org.rfcx.companion.R
import org.rfcx.companion.connection.socket.FileSocketManager
import org.rfcx.companion.connection.socket.GuardianSocketManager
import org.rfcx.companion.entity.guardian.Classifier
import org.rfcx.companion.entity.guardian.ClassifierLite
import org.rfcx.companion.entity.guardian.ClassifierSet
import org.rfcx.companion.entity.socket.request.InstructionCommand
import org.rfcx.companion.entity.socket.request.InstructionType
import org.rfcx.companion.localdb.ClassifierDb
import org.rfcx.companion.util.RealmHelper
import org.rfcx.companion.view.deployment.guardian.GuardianDeploymentProtocol

class ClassifierLoadFragment : Fragment(), ChildrenClickedListener {
    private var deploymentProtocol: GuardianDeploymentProtocol? = null
    var classifierLoadAdapter: ClassifierLoadAdapter? = null
    private var selectedFile: ClassifierItem.ClassifierVersion? = null
    private var selectedActivate: ClassifierLite? = null
    private var selectedDeActivate: ClassifierLite? = null
    private var loadingTimer: CountDownTimer? = null

    private var tempProgress = 0

    private val db by lazy { ClassifierDb(Realm.getInstance(RealmHelper.migrationConfig())) }

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
            classifiers,
            deploymentProtocol?.getClassifiers()
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
                val classifiers = deploymentProtocol?.getClassifiers()
                classifiers?.let {
                    classifierLoadAdapter?.classifierVersion = it
                    classifierLoadAdapter?.notifyDataSetChanged()
                    selectedFile?.let { selected ->
                        val selectedVersion = selected.classifier
                        val installedVersion = it[selected.classifier.id]
                        if (installedVersion != null && installedVersion.id == selectedVersion.id) {
                            classifierLoadAdapter?.hideProgressUploading()
                            nextButton.isEnabled = true
                            stopTimer()
                        }
                    }
                }
                val activeClassifiers = deploymentProtocol?.getActiveClassifiers()
                if (activeClassifiers != null) {
                    classifierLoadAdapter?.activeClassifierVersion = activeClassifiers
                    selectedActivate?.let { selected ->
                        val selectedId = selected.id
                        val activeId = activeClassifiers[selectedId]
                        if (activeId != null && activeId.id == selectedId) {
                            hideItemLoading()
                            selectedActivate = null
                        }
                    }

                    selectedDeActivate?.let { selected ->
                        val selectedId = selected.id
                        val activeId = activeClassifiers[selectedId]
                        if (activeId == null) {
                            hideItemLoading()
                            selectedDeActivate = null
                        }
                    }
                } else if (selectedDeActivate != null) {
                    hideItemLoading()
                    classifierLoadAdapter?.activeClassifierVersion = mapOf()
                    selectedDeActivate = null
                }
            }
        }

        FileSocketManager.uploadingProgress.observe(
            viewLifecycleOwner
        ) {
            requireActivity().runOnUiThread {
                if (it != tempProgress) {
                    tempProgress = it
                    classifierLoadAdapter?.progress = it
                }
            }
        }

        nextButton.setOnClickListener {
            deploymentProtocol?.nextStep()
        }

        val classifiers = db.getAll()
        if (classifiers.isNullOrEmpty()) {
            noClassifierText.visibility = View.VISIBLE
        } else {
            populateAdapterWithInfo(classifiers)
        }
    }

    private fun hideItemLoading() {
        classifierLoadAdapter?.hideSettingLoading()
        nextButton.isEnabled = true
        stopTimer()
    }

    private fun startTimer() {
        loadingTimer = object : CountDownTimer(120000, 1000) {
            override fun onTick(millisUntilFinished: Long) {}

            override fun onFinish() {
                classifierLoadAdapter?.let {
                    if (it.getProgressUploading()) {
                        it.hideProgressUploading()
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
        val classifier = db.get(selectedClassifier.classifier.id)
        classifier?.let {
            FileSocketManager.sendFile(classifier)
            nextButton.isEnabled = false
            startTimer()
        }
    }

    override fun onActiveClick(selectedClassifier: ClassifierLite) {
        nextButton.isEnabled = false
        selectedActivate = selectedClassifier
        GuardianSocketManager.sendInstructionMessage(
            InstructionType.SET,
            InstructionCommand.CLASSIFIER,
            Gson().toJson(ClassifierSet("activate", selectedClassifier.id))
        )
    }

    override fun onDeActiveClick(selectedClassifier: ClassifierLite) {
        nextButton.isEnabled = false
        selectedDeActivate = selectedClassifier
        GuardianSocketManager.sendInstructionMessage(
            InstructionType.SET,
            InstructionCommand.CLASSIFIER,
            Gson().toJson(ClassifierSet("deactivate", selectedClassifier.id))
        )
    }
}
