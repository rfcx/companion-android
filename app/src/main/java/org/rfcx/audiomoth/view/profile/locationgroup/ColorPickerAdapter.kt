package org.rfcx.audiomoth.view.profile.locationgroup

import android.graphics.Color
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.item_color_picker.view.*
import org.rfcx.audiomoth.R

class ColorPickerAdapter(private val onColorClickListener: (String) -> Unit) :
    RecyclerView.Adapter<ColorPickerAdapter.ColorPickerViewHolder>() {
    var items: ArrayList<String> = arrayListOf()
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ColorPickerViewHolder {
        val view = View.inflate(parent.context, R.layout.item_color_picker, null)
        return ColorPickerViewHolder(view)
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: ColorPickerViewHolder, position: Int) {
        holder.bind(items[position])
        holder.itemView.setOnClickListener {
            onColorClickListener(items[position])
        }
    }

    class ColorPickerViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val colorImageView = itemView.colorImageView

        fun bind(color: String) {
            val porterDuffColorFilter =
                PorterDuffColorFilter(Color.parseColor(color), PorterDuff.Mode.SRC_ATOP)
            colorImageView.drawable.colorFilter = porterDuffColorFilter
            colorImageView.setBackgroundColor(Color.TRANSPARENT)
        }
    }
}
