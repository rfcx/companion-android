package org.rfcx.audiomoth.view.deployment

import org.rfcx.audiomoth.entity.*

interface BaseDeploymentProtocol {
    fun startMapPicker(latitude: Double, longitude: Double, name: String)

    fun nextStep()
    fun backStep()

    fun getDeploymentLocation(): DeploymentLocation?

    fun setDeployLocation(locate: Locate)
}
