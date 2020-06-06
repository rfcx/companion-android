package org.rfcx.audiomoth.view.locate

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import kotlinx.android.synthetic.main.item_deploy_location.view.*
import org.rfcx.audiomoth.R
import org.rfcx.audiomoth.entity.LocateItem

class DeployLocationAdapter(context: Context, objects: Array<out LocateItem>) :
    ArrayAdapter<LocateItem>(context, 0, objects) {

    @SuppressLint("ViewHolder")
    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view =
            LayoutInflater.from(context).inflate(R.layout.item_deploy_location, parent, false)
        val textView = view.itemTextView
        val locateItem = getItem(position)
        textView.text = locateItem?.name ?: "NULL"
        return view
    }
}