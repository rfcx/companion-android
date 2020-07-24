package org.rfcx.audiomoth.extension

import android.widget.ImageView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.MultiTransformation
import jp.wasabeef.glide.transformations.BlurTransformation
import org.rfcx.audiomoth.R

fun ImageView.setDeploymentImage(url: String, blur: Boolean) {
    if (blur) {
        Glide.with(this)
            .load(url)
            .placeholder(R.drawable.bg_grey_light)
            .error(R.drawable.bg_grey_light)
            .transform(MultiTransformation(BlurTransformation(15, 1)))
            .into(this)
    } else {
        Glide.with(this)
            .load(url)
            .placeholder(R.drawable.bg_grey_light)
            .error(R.drawable.bg_grey_light)
            .into(this)
    }
}