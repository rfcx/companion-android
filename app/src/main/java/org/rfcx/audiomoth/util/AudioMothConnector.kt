package org.rfcx.audiomoth.util

import java.util.Calendar

interface AudioMothConnector {

    fun getBatteryState()

    fun getPacketLength(configuration: AudioMothConfiguration): Int

    fun setConfiguration(calendar: Calendar, configuration: AudioMothConfiguration, id: Array<Int>?)

}