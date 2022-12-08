package org.rfcx.companion.connection.socket

import androidx.lifecycle.MutableLiveData
import com.google.gson.Gson
import org.rfcx.companion.entity.socket.request.SocketRequest
import org.rfcx.companion.entity.socket.response.AdminPing
import org.rfcx.companion.util.socket.PingUtils
import java.io.DataInputStream
import java.io.DataOutputStream
import java.net.Socket

object AdminSocketManager {

    private var socket: Socket? = null
    private var outputStream: DataOutputStream? = null
    private var inputStream: DataInputStream? = null
    private var clientThread: Thread? = null // Thread for socket communication

    private lateinit var inComingMessageThread: Thread

    private val gson = Gson()

    val pingBlob = MutableLiveData<AdminPing>()

    // just to connect to server
    fun connect() {
        val data = Gson().toJson(
            SocketRequest(
                "connection"
            )
        )
        sendMessage(data)
    }

    private fun sendMessage(message: String) {
        clientThread = Thread {
            try {
                socket = Socket("192.168.43.1", 9997)
                socket?.keepAlive = true
                socket?.soTimeout = 10000
                startInComingMessageThread()
                outputStream = DataOutputStream(socket?.getOutputStream())
                outputStream?.writeUTF(message)
                outputStream?.flush()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        clientThread?.start()
    }

    private fun startInComingMessageThread() {
        inComingMessageThread = Thread {
            try {
                while (true) {
                    inputStream = DataInputStream(socket!!.getInputStream())
                    val dataInput = inputStream?.readUTF()
                    val message = PingUtils.unGzipString(dataInput)
                    if (!message.isNullOrBlank()) {
                        val ping = gson.fromJson(message, AdminPing::class.java)
                        pingBlob.postValue(ping)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        inComingMessageThread.start()
    }

    fun clearValue() {
        pingBlob.value = AdminPing()
    }

    fun stopConnection() {
        // stop incoming message thread
        if (::inComingMessageThread.isInitialized) {
            inComingMessageThread.interrupt()
        }

        // stop server thread
        clientThread?.interrupt()

        outputStream?.flush()
        outputStream?.close()

        inputStream?.close()

        outputStream?.close()
        socket?.close()
    }
}
