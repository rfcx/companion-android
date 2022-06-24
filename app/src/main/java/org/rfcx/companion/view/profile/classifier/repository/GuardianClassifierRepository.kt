package org.rfcx.companion.view.profile.classifier.repository

import org.rfcx.companion.repo.api.CoreApiHelper
import org.rfcx.companion.repo.local.LocalDataHelper

class GuardianClassifierRepository(
    private val coreApiHelper: CoreApiHelper,
    private val localDataHelper: LocalDataHelper
) {
    fun checkAvailableClassifiers(authUser: String) = coreApiHelper.checkAvailableClassifier(authUser)
    fun downloadClassifier(url: String) = coreApiHelper.downloadAPK(url)

    fun getDownloadedClassifier() = localDataHelper.getClassifierLocalDb().getAll()
    fun getDownloadedClassifierLiveData() = localDataHelper.getClassifierLocalDb().getAllAsLiveData()
}
