package org.rfcx.audiomoth.extension

import android.widget.ImageView
import com.bumptech.glide.Glide
import org.rfcx.audiomoth.R

fun ImageView.setDeploymentImage(url: String) {
    Glide.with(context)
        .load(url)
        .placeholder(R.drawable.bg_grey_light)
        .error(R.drawable.bg_grey_light)
        .into(this)
}