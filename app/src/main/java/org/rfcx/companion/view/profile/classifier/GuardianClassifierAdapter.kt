package org.rfcx.companion.view.profile.classifier

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.item_classifier_download.view.*
import org.rfcx.companion.R
import org.rfcx.companion.entity.File
import org.rfcx.companion.entity.guardian.Classifier
import org.rfcx.companion.entity.response.GuardianClassifierResponse
import org.rfcx.companion.util.file.FileStatus

class GuardianClassifierAdapter(private val listener: ClassifierListener): RecyclerView.Adapter<GuardianClassifierAdapter.GuardianClassifierViewHolder>() {

    var availableClassifiers: List<File> = listOf()
        set(value) {
            field = value
            notifyDataSetChanged()
        }
    var downloadedClassifiers: List<Classifier> = listOf()
        set(value) {
            field = value
        }

    var needLoading = false
    var selected = -1

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): GuardianClassifierViewHolder {
        val view =
            LayoutInflater.from(parent.context).inflate(R.layout.item_classifier_download, parent, false)
        return GuardianClassifierViewHolder(view)
    }

    override fun onBindViewHolder(holder: GuardianClassifierViewHolder, position: Int) {
        holder.bind(availableClassifiers[position])
        holder.deleteButton.setOnClickListener {
            selected = position
            listener.onDeleteClicked(availableClassifiers[position].file as GuardianClassifierResponse)
        }
        holder.downloadButton.setOnClickListener {
            selected = position
            needLoading = true
            notifyDataSetChanged()
            listener.onDownloadClicked(availableClassifiers[position].file as GuardianClassifierResponse)
        }
    }

    override fun getItemCount(): Int = availableClassifiers.size

    inner class GuardianClassifierViewHolder(itemView: View): RecyclerView.ViewHolder(itemView) {
        private val name = itemView.classifierName
        private val status = itemView.classifierStatus
        val downloadButton: Button = itemView.classifierDownloadButton
        val deleteButton: Button = itemView.classifierDeleteButton
        private val loading = itemView.downloadLoading

        fun bind(file: File) {
            val classifier = (file.file as GuardianClassifierResponse)
            val downloadedClassifier = downloadedClassifiers.findLast { it.name == classifier.name }
            name.text = classifier.name
            when(file.status) {
                FileStatus.NOT_DOWNLOADED -> {
                    status.visibility = View.VISIBLE
                    downloadButton.isEnabled = true
                    downloadButton.visibility = View.VISIBLE
                    deleteButton.visibility = View.GONE
                }
                FileStatus.NEED_UPDATE -> {
                    status.visibility = View.GONE
                    downloadButton.isEnabled = true
                    downloadButton.visibility = View.VISIBLE
                    deleteButton.isEnabled = true
                    deleteButton.visibility = View.VISIBLE
                }
                FileStatus.UP_TO_DATE -> {
                    status.visibility = View.GONE
                    downloadButton.visibility = View.VISIBLE
                    downloadButton.isEnabled = false
                    downloadButton.text = itemView.context.getString(R.string.up_to_date)
                    deleteButton.isEnabled = true
                    deleteButton.visibility = View.VISIBLE
                    deleteButton.text = "delete v${downloadedClassifier!!.version}"
                }
            }

            if (needLoading && adapterPosition != selected) {
                downloadButton.isEnabled = false
                deleteButton.isEnabled = false
                loading.visibility = View.GONE
            } else if (needLoading && adapterPosition == selected) {
                downloadButton.visibility = View.GONE
                deleteButton.visibility = View.GONE
                loading.visibility = View.VISIBLE
            } else {
                loading.visibility = View.GONE
            }

        }
    }
}

interface ClassifierListener {
    fun onDownloadClicked(classifier: GuardianClassifierResponse)
    fun onDeleteClicked(classifier: GuardianClassifierResponse)
}

