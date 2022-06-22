package org.rfcx.companion.view.deployment.guardian.classifier

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.expandable_child_item.view.*
import kotlinx.android.synthetic.main.expandable_parent_item.view.*
import org.rfcx.companion.R
import org.rfcx.companion.entity.guardian.Classifier
import org.rfcx.companion.util.file.APKUtils.calculateVersionValue

class ClassifierLoadAdapter(
    private var childrenClickedListener: ChildrenClickedListener,
    classifiers: List<Classifier>
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        const val VERSION_ITEM = 1
        const val HEADER_ITEM = 2
    }

    private var classifierStateModelList = mutableListOf<ClassifierItem>()

    var classifierVersion = mapOf<String, String>()

    private var needLoading = false

    init {
        val classifierGrouped = classifiers.sortedBy { it.name }.groupBy { it.name }
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
                        R.layout.expandable_child_item, parent, false
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
                val versionItem = (classifierStateModelList[position] as ClassifierItem.ClassifierVersion)
                (holder as ClassifierVersionViewHolder).modelVersion.text = versionItem.classifier.version
                holder.modelVersion.apply {
                    val installedVersion = classifierVersion[versionItem.classifier.name]
                    holder.modelInstalled.text = context.getString(R.string.installed_software, installedVersion)
                    if (!needLoading) {
                        if (installedVersion != null && calculateVersionValue(installedVersion) >= calculateVersionValue(versionItem.classifier.version)) {
                            holder.modelSendButton.visibility = View.GONE
                            holder.modelUpToDateText.visibility = View.VISIBLE
                        } else {
                            holder.modelSendButton.visibility = View.VISIBLE
                            holder.modelSendButton.text = "update to ${versionItem.classifier.version}"
                            holder.modelUpToDateText.visibility = View.GONE
                        }
                    }
                }

                holder.modelSendButton.isEnabled = !needLoading
                if (!needLoading && holder.modelLoading.visibility == View.VISIBLE) {
                    holder.modelLoading.visibility = View.GONE
                }
                holder.modelSendButton.setOnClickListener {
                    showLoading()
                    it.visibility = View.GONE
                    holder.modelLoading.visibility = View.VISIBLE
                    childrenClickedListener.onItemClick(versionItem)
                }
            }
            else -> {
                val headerItem = (classifierStateModelList[position] as ClassifierItem.ClassifierHeader)
                (holder as ClassifierHeaderViewHolder).modelName.text = headerItem.name
            }
        }
    }

    fun showLoading() {
        needLoading = true
        notifyDataSetChanged()
    }

    fun hideLoading() {
        needLoading = false
        notifyDataSetChanged()
    }

    fun getLoading(): Boolean = needLoading

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
        internal var modelInstalled: TextView = itemView.fileInstalledVersionTextView
        internal var modelSendButton: Button = itemView.fileSendButton
        internal var modelUpToDateText: TextView = itemView.fileUpToDateTextView
        internal var modelLoading: ProgressBar = itemView.fileLoading
    }
}

sealed class ClassifierItem {
    data class ClassifierHeader(val name: String) : ClassifierItem()
    data class ClassifierVersion(val classifier: Classifier) : ClassifierItem()
}

interface ChildrenClickedListener {
    fun onItemClick(selectedClassifier: ClassifierItem.ClassifierVersion)
}
