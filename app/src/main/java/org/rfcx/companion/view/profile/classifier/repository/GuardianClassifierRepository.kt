package org.rfcx.companion.view.profile.classifier.repository

import org.rfcx.companion.entity.guardian.Classifier
import org.rfcx.companion.repo.api.CoreApiHelper
import org.rfcx.companion.repo.api.DeviceApiHelper
import org.rfcx.companion.repo.local.LocalDataHelper

class GuardianClassifierRepository(
    private val deviceApiHelper: DeviceApiHelper,
    private val localDataHelper: LocalDataHelper
) {
    fun checkAvailableClassifiers(authUser: String) = deviceApiHelper.checkAvailableClassifier(authUser)
    fun downloadClassifier(url: String) = deviceApiHelper.downloadAPK(url)

    fun getDownloadedClassifier() = localDataHelper.getClassifierLocalDb().getAll()
    fun getDownloadedClassifierLiveData() = localDataHelper.getClassifierLocalDb().getAllAsLiveData()

    fun saveClassifier(classifier: Classifier) {
        localDataHelper.getClassifierLocalDb().insert(classifier)
    }

    fun deleteClassifier(id: String) {
        localDataHelper.getClassifierLocalDb().delete(id)
    }
}
