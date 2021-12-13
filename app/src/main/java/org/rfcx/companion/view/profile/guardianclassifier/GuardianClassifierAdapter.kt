package org.rfcx.companion.view.profile.guardianclassifier

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.widget.AppCompatButton
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.item_classifier.view.*
import org.rfcx.companion.R
import org.rfcx.companion.entity.socket.Classifier

class GuardianClassifierAdapter(
    private var classifierLoadListener: ClassifierDownloadListener
) :
    RecyclerView.Adapter<GuardianClassifierAdapter.GuardianClassifierViewHolder>() {

    private var needLoading = false

    var items: List<Classifier> = arrayListOf()
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    fun getLoading() = needLoading

    fun showLoading() {
        needLoading = true
        notifyDataSetChanged()
    }

    fun hideLoading() {
        needLoading = false
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GuardianClassifierViewHolder {
        val view =
            LayoutInflater.from(parent.context).inflate(R.layout.item_classifier, parent, false)
        return GuardianClassifierViewHolder(view)
    }

    override fun onBindViewHolder(holder: GuardianClassifierViewHolder, position: Int) {
        holder.setText(items[position].name)

        holder.classifierTextView.isEnabled = !needLoading
        holder.classifierLoadButton.setOnClickListener {
            showLoading()
            it.visibility = View.GONE
            holder.classifierLoading.visibility = View.VISIBLE
            classifierLoadListener.onDownloadClicked(items[position].path)
        }
    }

    override fun getItemCount(): Int = items.size

    inner class GuardianClassifierViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val classifierTextView: TextView = itemView.classifierTextView
        val classifierLoadButton: AppCompatButton = itemView.classifierLoadButton
        val classifierLoading: ProgressBar = itemView.classifierLoading

        fun setText(name: String) {
            classifierTextView.text = name
        }
    }
}

interface ClassifierDownloadListener {
    fun onDownloadClicked(path: String)
}
