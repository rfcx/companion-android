package org.rfcx.companion.view.profile.guardiansoftware.repository

import org.rfcx.companion.repo.api.CoreApiHelper

class GuardianSoftwareRepository(
    private val coreApiHelper: CoreApiHelper
) {
    fun checkSoftwareVersion() = coreApiHelper.checkSoftwareVersion()
    fun downloadAPK(url: String) = coreApiHelper.downloadAPK(url)
}
