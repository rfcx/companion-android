package org.rfcx.companion.view.deployment.guardian.classifierloader

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.widget.AppCompatButton
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.item_classifier.view.*
import kotlinx.android.synthetic.main.item_location_group.view.*
import org.rfcx.companion.R
import org.rfcx.companion.entity.Project
import org.rfcx.companion.entity.socket.Classifier
import org.rfcx.companion.view.deployment.guardian.softwareupdate.ChildrenClickedListener

class ClassifierAdapter(
    private var classifierLoadListener: ClassifierLoadListener
) :
    RecyclerView.Adapter<ClassifierAdapter.ClassifierViewHolder>() {

    private var needLoading = false

    var items: List<Classifier> = arrayListOf()
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    fun showLoading() {
        needLoading = true
        notifyDataSetChanged()
    }

    fun hideLoading() {
        needLoading = false
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ClassifierViewHolder {
        val view =
            LayoutInflater.from(parent.context).inflate(R.layout.item_classifier, parent, false)
        return ClassifierViewHolder(view)
    }

    override fun onBindViewHolder(holder: ClassifierViewHolder, position: Int) {
        holder.setText(items[position].name)

        holder.classifierTextView.isEnabled = !needLoading
        holder.classifierLoadButton.setOnClickListener {
            showLoading()
            it.visibility = View.GONE
            holder.classifierLoading.visibility = View.VISIBLE
            classifierLoadListener.onLoadClicked()
        }
    }

    override fun getItemCount(): Int = items.size

    inner class ClassifierViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val classifierTextView: TextView = itemView.classifierTextView
        val classifierLoadButton: AppCompatButton = itemView.classifierLoadButton
        val classifierLoading: ProgressBar = itemView.classifierLoading

        fun setText(name: String) {
            classifierTextView.text = name
        }
    }
}

interface ClassifierLoadListener {
    fun onLoadClicked()
}
