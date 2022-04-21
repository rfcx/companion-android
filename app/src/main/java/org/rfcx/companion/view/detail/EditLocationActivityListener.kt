package org.rfcx.companion.view.detail

import org.rfcx.companion.entity.Project
import org.rfcx.companion.entity.Stream

interface EditLocationActivityListener {
    fun startMapPickerPage(latitude: Double, longitude: Double, altitude: Double, streamId: Int)
    fun updateDeploymentDetail(name: String, altitude: Double)

    fun getStream(id: Int): Stream
    fun getProject(id: Int): Project

    fun startLocationGroupPage()

    fun showAppbar()
    fun hideAppbar()
}
