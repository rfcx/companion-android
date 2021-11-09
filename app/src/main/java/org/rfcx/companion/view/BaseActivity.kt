package org.rfcx.companion.view

import androidx.appcompat.app.AppCompatActivity
import org.rfcx.companion.view.deployment.AudioMothDeploymentActivity
import org.rfcx.companion.view.dialog.LoadingDialogFragment

open class BaseActivity : AppCompatActivity() {
    fun showLoading() {
        val loadingDialog: LoadingDialogFragment =
            supportFragmentManager.findFragmentByTag(AudioMothDeploymentActivity.loadingDialogTag) as LoadingDialogFragment?
                ?: run {
                    LoadingDialogFragment()
                }
        loadingDialog.show(supportFragmentManager, AudioMothDeploymentActivity.loadingDialogTag)
    }

    fun hideLoading() {
        val loadingDialog: LoadingDialogFragment? =
            supportFragmentManager.findFragmentByTag(AudioMothDeploymentActivity.loadingDialogTag) as LoadingDialogFragment?
        loadingDialog?.dismissDialog()
    }
}
