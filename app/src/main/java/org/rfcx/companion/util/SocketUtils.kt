package org.rfcx.companion.util

import org.rfcx.companion.connection.socket.AdminSocketManager
import org.rfcx.companion.connection.socket.AudioCastSocketManager
import org.rfcx.companion.connection.socket.FileSocketManager
import org.rfcx.companion.connection.socket.GuardianSocketManager

object SocketUtils {

    fun stopAllConnections() {
        GuardianSocketManager.stopConnection()
        AdminSocketManager.stopConnection()
        AudioCastSocketManager.stopConnection()
        FileSocketManager.stopConnection()
    }

    fun clearAllBlobs() {
        GuardianSocketManager.clearValue()
        AdminSocketManager.clearValue()
    }
}
