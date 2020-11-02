package org.rfcx.companion.view

import androidx.appcompat.app.AppCompatActivity
import org.rfcx.companion.view.deployment.EdgeDeploymentActivity
import org.rfcx.companion.view.dialog.LoadingDialogFragment

open class BaseActivity : AppCompatActivity() {
    fun showLoading() {
        val loadingDialog: LoadingDialogFragment =
            supportFragmentManager.findFragmentByTag(EdgeDeploymentActivity.loadingDialogTag) as LoadingDialogFragment?
                ?: run {
                    LoadingDialogFragment()
                }
        loadingDialog.show(supportFragmentManager, EdgeDeploymentActivity.loadingDialogTag)
    }

    fun hideLoading() {
        val loadingDialog: LoadingDialogFragment? =
            supportFragmentManager.findFragmentByTag(EdgeDeploymentActivity.loadingDialogTag) as LoadingDialogFragment?
        loadingDialog?.dismissDialog()
    }
}