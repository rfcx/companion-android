/****************************************************************************
 * AudioMothConnector.kt
 * openacousticdevices.info
 * June 2020
 *****************************************************************************/

package org.rfcx.audiomoth.util

import java.util.Calendar

interface AudioMothConnector {

    fun getBatteryState()

    fun getPacketLength(configuration: AudioMothConfiguration): Int

    fun setConfiguration(calendar: Calendar, configuration: AudioMothConfiguration, id: DeploymentIdentifier?)
}
