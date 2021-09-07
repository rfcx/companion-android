package org.rfcx.companion.connection.socket

import android.util.Log
import androidx.lifecycle.MutableLiveData
import com.google.gson.Gson
import org.rfcx.companion.entity.socket.response.AudioCastPing
import org.rfcx.companion.entity.socket.response.FileSendingResult
import org.rfcx.companion.util.MicrophoneTestUtils
import org.rfcx.companion.util.file.APKUtils
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.File
import java.io.InputStream
import java.net.Socket

object FileSocketManager {

    private var socket: Socket? = null
    private var outputStream: DataOutputStream? = null
    private var inputStream: DataInputStream? = null
    private var clientThread: Thread? = null // Thread for socket communication

    private lateinit var inComingMessageThread: Thread

    private val gson = Gson()

    fun sendFiles(filePaths: List<String>) {
        filePaths.forEach {
            Log.d("FILESOCKET", "SENDING FILE")
            sendMessage(APKUtils.getAPKFileFromPath(it))
        }
    }

    private fun sendMessage(file: File) {
        clientThread = Thread {
            try {
                socket = Socket("192.168.43.1", 9996)
                socket?.keepAlive = true
                startInComingMessageThread()
                outputStream = DataOutputStream(socket?.getOutputStream())
                outputStream?.write(file.name.toByteArray())
                outputStream?.write("|".toByteArray())
                outputStream?.write(file.readBytes())
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
                    if (!dataInput.isNullOrBlank()) {

                        val sendResult = gson.fromJson(dataInput, FileSendingResult::class.java)

                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        inComingMessageThread.start()
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
