package org.rfcx.companion.extension

import android.widget.ImageView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.MultiTransformation
import com.bumptech.glide.load.model.GlideUrl
import com.bumptech.glide.load.model.LazyHeaders
import jp.wasabeef.glide.transformations.BlurTransformation
import org.rfcx.companion.R

fun ImageView.setDeploymentImage(url: String, blur: Boolean, fromServer: Boolean, token: String? = null) {
    if (fromServer) {
        val glideUrl = GlideUrl(
            url,
            LazyHeaders.Builder()
                .addHeader("Authorization", "Bearer $token")
                .build()
        )

        Glide.with(this)
            .load(glideUrl)
            .placeholder(R.drawable.bg_grey_light)
            .error(R.drawable.bg_grey_light)
            .into(this)

    } else {
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
}
