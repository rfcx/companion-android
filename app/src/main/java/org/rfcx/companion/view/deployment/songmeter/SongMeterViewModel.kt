package org.rfcx.companion.view.deployment.songmeter

import android.annotation.SuppressLint
import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import org.rfcx.companion.entity.Deployment
import org.rfcx.companion.entity.Locate
import org.rfcx.companion.entity.Project
import org.rfcx.companion.entity.guardian.GuardianDeployment
import org.rfcx.companion.util.Resource

class SongMeterViewModel(
    application: Application,
    private val songMeterRepository: SongMeterRepository
) : AndroidViewModel(application) {
    @SuppressLint("StaticFieldLeak")
    private val context = getApplication<Application>().applicationContext
    private val deployments = MutableLiveData<Resource<List<Deployment>>>()
    private val guardianDeployments = MutableLiveData<Resource<List<GuardianDeployment>>>()
    private val locates = MutableLiveData<Resource<List<Locate>>>()

    fun getDeploymentsFromRemote(): LiveData<Resource<List<Deployment>>> {
        return deployments
    }

    fun getGuardianDeploymentsFromRemote(): LiveData<Resource<List<GuardianDeployment>>> {
        return guardianDeployments
    }

    fun getDeploymentsFromLocal(): List<Deployment> {
        return songMeterRepository.getDeploymentFromLocal()
    }

    fun getGuardianDeploymentsFromLocal(): List<GuardianDeployment> {
        return songMeterRepository.getGuardianDeploymentFromLocal()
    }

    fun getLocatesFromLocal(): List<Locate> {
        return songMeterRepository.getLocateFromLocal()
    }

    fun getProjectByName(name: String): Project? {
        return songMeterRepository.getProjectByName(name)
    }
    fun setLocateInsertOrUpdate(locate: Locate){
        songMeterRepository.setLocateInsertOrUpdate(locate)
    }
}
