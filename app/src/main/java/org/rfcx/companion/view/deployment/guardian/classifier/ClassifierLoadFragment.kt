package org.rfcx.companion.view.deployment.guardian.classifier

import android.content.Context
import android.os.Bundle
import android.os.CountDownTimer
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
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
import org.rfcx.companion.util.file.APKUtils
import org.rfcx.companion.view.deployment.guardian.GuardianDeploymentProtocol

class ClassifierLoadFragment : Fragment(), ChildrenClickedListener {
    private var deploymentProtocol: GuardianDeploymentProtocol? = null
    var classifierLoadAdapter: ClassifierLoadAdapter? = null
    private var selectedFile: ClassifierItem.ClassifierVersion? = null
    private var selectedActivate: ClassifierLite? = null
    private var selectedDeActivate: ClassifierLite? = null
    private var otherActive: Map<String, ClassifierLite>? = null
    private var loadingTimer: CountDownTimer? = null

    private var tempProgress = 0

    private val REQUIRED_VERSION = 10100

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

        if (!isSoftwareCompatible()) {
            showAlert(getString(R.string.guardian_software_not_allowed))
        }

        if (!isSMSOrSatGuardian()) {
            showAlert(getString(R.string.guardian_type_not_allowed))
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
                when {
                    activeClassifiers != null -> {
                        classifierLoadAdapter?.activeClassifierVersion = activeClassifiers

                        if (selectedActivate != null) {
                            val selectedId = selectedActivate?.id
                            activeClassifiers[selectedId]?.let {
                                selectedActivate = null
                            }
                        }

                        if (selectedActivate == null && otherActive == null && selectedDeActivate == null) {
                            hideItemLoading()
                            solarWarnTextView.visibility = View.GONE
                            nextButton.isEnabled = true
                        } else {
                            otherActive?.forEach {
                                if (activeClassifiers[it.key] == null) {
                                    hideItemLoading()
                                    solarWarnTextView.visibility = View.GONE
                                    nextButton.isEnabled = true
                                }
                            }
                        }
                    }
                    selectedDeActivate != null -> {
                        hideItemLoading()
                        classifierLoadAdapter?.activeClassifierVersion = mapOf()
                        selectedDeActivate = null
                        solarWarnTextView.visibility = View.VISIBLE
                        nextButton.isEnabled = false
                    }
                    else -> {
                        solarWarnTextView.visibility = View.VISIBLE
                        nextButton.isEnabled = false
                    }
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

    private fun isSoftwareCompatible(): Boolean {
        val software = deploymentProtocol?.getSoftwareVersion() ?: return false

        if (!software.containsKey("guardian")) return false
        val guardian = software["guardian"] ?: return false
        if (APKUtils.calculateVersionValue(guardian) < REQUIRED_VERSION) return false

        if (!software.containsKey("classify")) return false
        val classify = software["classify"] ?: return false
        if (APKUtils.calculateVersionValue(classify) < REQUIRED_VERSION) return false

        return true
    }

    private fun isSMSOrSatGuardian(): Boolean {
        return deploymentProtocol?.isSMSOrSatGuardian() ?: return false
    }

    private fun showAlert(text: String) {
        val dialogBuilder: AlertDialog.Builder =
            AlertDialog.Builder(requireContext()).apply {
                setTitle(null)
                setMessage(text)
                setPositiveButton(R.string.go_back) { _, _ ->
                    deploymentProtocol?.backStep()
                }
            }
        dialogBuilder.create().show()
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
        otherActive = deploymentProtocol?.getActiveClassifiers()?.filter { it.key != selectedClassifier.id }
        GuardianSocketManager.sendInstructionMessage(
            InstructionType.SET,
            InstructionCommand.CLASSIFIER,
            Gson().toJson(ClassifierSet("activate", selectedClassifier.id))
        )
    }

    override fun onDeActiveClick(selectedClassifier: ClassifierLite) {
        nextButton.isEnabled = false
        selectedDeActivate = selectedClassifier
        otherActive = deploymentProtocol?.getActiveClassifiers()
        GuardianSocketManager.sendInstructionMessage(
            InstructionType.SET,
            InstructionCommand.CLASSIFIER,
            Gson().toJson(ClassifierSet("deactivate", selectedClassifier.id))
        )
    }
}
