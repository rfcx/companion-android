package org.rfcx.companion.util

import android.app.Activity
import android.content.DialogInterface
import androidx.annotation.StringRes
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import org.rfcx.companion.R

fun Activity.showCommonDialog(message: String) {
    showCommonDialog(null, message)
}

fun Activity.showCommonDialog(
    title: String? = null,
    message: String,
    onClick: DialogInterface.OnClickListener? = null
) {
    if (isDestroyed || isFinishing) return

    MaterialAlertDialogBuilder(this, R.style.BaseAlertDialog)
        .setTitle(title)
        .setMessage(message)
        .setPositiveButton(getString(R.string.ok), onClick)
        .show()
}

fun Activity.showCommonDialog(@StringRes message: Int) {
    showCommonDialog(null, getString(message))
}

fun Activity.showCommonDialog(
    @StringRes title: Int,
    @StringRes message: Int,
    onClick: DialogInterface.OnClickListener? = null
) {
    showCommonDialog(getString(title), getString(message), onClick)
}
