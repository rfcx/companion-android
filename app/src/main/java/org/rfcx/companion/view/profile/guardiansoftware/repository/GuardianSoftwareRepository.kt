package org.rfcx.companion.view.profile.guardiansoftware.repository

import org.rfcx.companion.repo.api.CoreApiHelper

class GuardianSoftwareRepository(
    private val coreApiHelper: CoreApiHelper
) {

    fun checkSoftwareVersion(authUser: String) = coreApiHelper.checkSoftwareVersion(authUser)

}
