package org.rfcx.companion.view.deployment.guardian.classifier

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.progressindicator.LinearProgressIndicator
import kotlinx.android.synthetic.main.classifier_item.view.*
import kotlinx.android.synthetic.main.expandable_parent_item.view.*
import org.rfcx.companion.R
import org.rfcx.companion.entity.guardian.Classifier
import org.rfcx.companion.entity.guardian.ClassifierLite

class ClassifierLoadAdapter(
    private var childrenClickedListener: ChildrenClickedListener,
    classifiers: List<Classifier>,
    installedClassifiers: Map<String, ClassifierLite>?
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        const val VERSION_ITEM = 1
        const val HEADER_ITEM = 2
    }

    private var classifierStateModelList = mutableListOf<ClassifierItem>()

    var classifierVersion = mapOf<String, ClassifierLite>()
    var activeClassifierVersion = mapOf<String, ClassifierLite>()

    private var isSettingActivation = false
    private var isUploading = false
    var progress = 0
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    init {
        val bothClassifiers = combineDownloadAndInstallClassifier(classifiers, installedClassifiers)
        val classifierGrouped = bothClassifiers.sortedBy { it.name }.groupBy { it.name }
        classifierGrouped.forEach { clsf ->
            classifierStateModelList.add(ClassifierItem.ClassifierHeader(clsf.key))
            clsf.value.forEach {
                classifierStateModelList.add(ClassifierItem.ClassifierVersion(it))
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            VERSION_ITEM -> {
                ClassifierVersionViewHolder(
                    LayoutInflater.from(parent.context).inflate(
                        R.layout.classifier_item, parent, false
                    )
                )
            }

            else -> {
                ClassifierHeaderViewHolder(
                    LayoutInflater.from(parent.context).inflate(
                        R.layout.expandable_parent_item, parent, false
                    )
                )
            }
        }
    }

    override fun getItemCount(): Int = classifierStateModelList.size

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder.itemViewType) {
            VERSION_ITEM -> {
                val versionItem =
                    (classifierStateModelList[position] as ClassifierItem.ClassifierVersion)
                (holder as ClassifierVersionViewHolder).modelVersion.text =
                    "version ${versionItem.classifier.version}"

                val installedVersion =
                    classifierVersion[versionItem.classifier.id]

                if (!isUploading) {
                    if (installedVersion != null && installedVersion.version.toInt() >= versionItem.classifier.version.toInt()) {
                        holder.modelSendButton.visibility = View.INVISIBLE
                    } else {
                        holder.modelSendButton.visibility = View.VISIBLE
                        holder.modelSendButton.text =
                            "load version ${versionItem.classifier.version}"
                    }
                }

                if (installedVersion != null) {
                    val activeInstalled = activeClassifierVersion[installedVersion.id]
                    if (!isSettingActivation) {
                        if (activeInstalled != null) {
                            holder.hideSettingLoading()
                            holder.modelDeActiveButton.visibility = View.VISIBLE
                            holder.modelActiveButton.visibility = View.GONE
                        } else {
                            holder.hideSettingLoading()
                            holder.modelDeActiveButton.visibility = View.GONE
                            holder.modelActiveButton.visibility = View.VISIBLE
                        }
                    }
                }

                if (!isUploading && holder.modelProgress.visibility == View.VISIBLE) {
                    holder.hideUploadingProgress()
                }

                holder.modelSendButton.isEnabled = !isUploading
                holder.modelSendButton.setOnClickListener {
                    showProgressUploading()
                    it.visibility = View.GONE
                    holder.showUploadingProgress()
                    childrenClickedListener.onItemClick(versionItem)
                }

                holder.modelActiveButton.isEnabled = !isSettingActivation
                holder.modelActiveButton.setOnClickListener {
                    showSettingLoading()
                    holder.modelActiveButton.visibility = View.GONE
                    holder.showSettingLoading()
                    childrenClickedListener.onActiveClick(installedVersion!!)
                }

                holder.modelDeActiveButton.isEnabled = !isSettingActivation
                holder.modelDeActiveButton.setOnClickListener {
                    showSettingLoading()
                    holder.modelDeActiveButton.visibility = View.GONE
                    holder.showSettingLoading()
                    childrenClickedListener.onDeActiveClick(installedVersion!!)
                }

                if (progress != 100) {
                    holder.setProgress(progress)
                } else {
                    holder.modelProgress.isIndeterminate = true
                }
            }
            else -> {
                val headerItem =
                    (classifierStateModelList[position] as ClassifierItem.ClassifierHeader)
                (holder as ClassifierHeaderViewHolder).modelName.text = headerItem.name
            }
        }
    }

    private fun combineDownloadAndInstallClassifier(downloads: List<Classifier>, installs: Map<String, ClassifierLite>?): List<ClassifierLite> {
        if (installs != null) {
            val installed = installs.map { ClassifierLite(it.value.id, it.value.name, it.value.version) }.filter { downloads.find { dwnl -> dwnl.id == it.id } == null }
            val notInstalled = downloads.filter { installed.find { ins -> ins.id == it.id } == null }.map { ClassifierLite(it.id, it.name, it.version) }
            return installed + notInstalled
        }
        return downloads.map { ClassifierLite(it.id, it.name, it.version) }
    }

    private fun showProgressUploading() {
        isUploading = true
        notifyDataSetChanged()
    }

    fun hideProgressUploading() {
        isUploading = false
        notifyDataSetChanged()
    }

    fun getProgressUploading(): Boolean = isUploading

    private fun showSettingLoading() {
        isSettingActivation = true
        notifyDataSetChanged()
    }

    fun hideSettingLoading() {
        isSettingActivation = false
        notifyDataSetChanged()
    }

    override fun getItemViewType(position: Int): Int {
        return when (classifierStateModelList[position]) {
            is ClassifierItem.ClassifierVersion -> VERSION_ITEM
            else -> HEADER_ITEM
        }
    }

    class ClassifierHeaderViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        internal var layout = itemView.country_item_parent_container
        internal var modelName: TextView = itemView.fileNameTextView
    }

    class ClassifierVersionViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        internal var layout = itemView.country_item_child_container
        internal var modelVersion: TextView = itemView.fileVersionTextView
        internal var modelSendButton: Button = itemView.fileSendButton
        internal var modelLoading: ProgressBar = itemView.fileLoading
        internal var modelProgress: LinearProgressIndicator = itemView.uploadingProgress
        internal var modelActiveButton: Button = itemView.classifierActivateButton
        internal var modelDeActiveButton: Button = itemView.classifierDeActivateButton

        fun showSettingLoading() {
            modelLoading.visibility = View.VISIBLE
        }

        fun hideSettingLoading() {
            modelLoading.visibility = View.GONE
        }

        fun showUploadingProgress() {
            modelProgress.visibility = View.VISIBLE
        }

        fun hideUploadingProgress() {
            modelProgress.visibility = View.GONE
        }

        fun setProgress(value: Int) {
            modelProgress.setProgressCompat(value, true)
        }
    }
}

sealed class ClassifierItem {
    data class ClassifierHeader(val name: String) : ClassifierItem()
    data class ClassifierVersion(val classifier: ClassifierLite) : ClassifierItem()
}

interface ChildrenClickedListener {
    fun onItemClick(selectedClassifier: ClassifierItem.ClassifierVersion)
    fun onActiveClick(selectedClassifier: ClassifierLite)
    fun onDeActiveClick(selectedClassifier: ClassifierLite)
}
