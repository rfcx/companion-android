package org.rfcx.audiomoth.connection.socket

import android.util.Log
import com.google.gson.Gson
import org.json.JSONObject
import org.rfcx.audiomoth.entity.socket.*
import java.io.DataInputStream
import java.io.DataOutputStream
import java.net.Socket

object SocketManager {

    private lateinit var socket: Socket
    private lateinit var outputStream: DataOutputStream
    private lateinit var clientThread: Thread

    private val gson = Gson()

    private val LOGTAG = "Client-SocketManager"

    fun connect(onReceiveRespoonse: OnReceiveResponse) {
        val data = gson.toJson(SocketRequest(SocketType.CONNECTION.name))
        sendData(data, onReceiveRespoonse)
    }

    fun getDiagnosticData(onReceiveRespoonse: OnReceiveResponse) {
        val data = gson.toJson(SocketRequest(SocketType.DIAGNOSTIC.name))
        sendData(data, onReceiveRespoonse)
    }

    fun getCurrentConfiguration(onReceiveRespoonse: OnReceiveResponse) {
        val data = gson.toJson(SocketRequest(SocketType.CONFIGURE.name))
        sendData(data, onReceiveRespoonse)
    }

    private fun sendData(data: String, onReceiveRespoonse: OnReceiveResponse) {
        clientThread = Thread(Runnable {
            try {
                socket = Socket("192.168.1.43", 9090)

                while (true) {
                    outputStream = DataOutputStream(socket.getOutputStream())

                    outputStream.writeUTF(data)
                    outputStream.flush()

                    //receiving data
                    val dataInput = DataInputStream(socket.getInputStream()).readUTF()
                    if (!dataInput.isNullOrBlank()) {

                        Log.d(LOGTAG, "Receiving data from Server: $dataInput")

                        val receiveJson = JSONObject(dataInput)
                        val jsonIterator = receiveJson.keys()

                        val keys = jsonIterator.asSequence().toList()
                        when (keys[0].toString()) {
                            SocketType.CONFIGURE.name -> {
                                val response = gson.fromJson(dataInput, ConfigureResponse::class.java)
                                Log.d(LOGTAG, "Configure response: ${response.configure}")
                            }
                            SocketType.DIAGNOSTIC.name -> {
                                val response = gson.fromJson(dataInput, DiagnosticResponse::class.java)
                                Log.d(LOGTAG, "Diagnostic response: ${response.diagnostic}")
                                onReceiveRespoonse.onReceive(response)
                            }
                            SocketType.CONNECTION.name -> {
                                val response = gson.fromJson(dataInput, ConnectionResponse::class.java)
                                Log.d(LOGTAG, "Connection status: ${response.connection.status}")
                                onReceiveRespoonse.onReceive(response)
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(LOGTAG, e.toString())
            }
        })

        clientThread.start()
    }

    fun stopConnection() {
        socket.close()
        outputStream.close()
        clientThread.interrupt()
    }

}

interface OnReceiveResponse {
    fun onReceive(response: SocketResposne)
}

enum class SocketType(value: String) {
    CONNECTION("connection"),
    DIAGNOSTIC("diagnostic"),
    CONFIGURE("configure")
}
