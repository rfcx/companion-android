package org.rfcx.audiomoth.connection.socket

import com.google.gson.Gson
import org.json.JSONObject
import org.rfcx.audiomoth.entity.socket.*
import java.io.DataInputStream
import java.io.DataOutputStream
import java.net.ConnectException
import java.net.Socket

object SocketManager {

    private lateinit var socket: Socket
    private lateinit var outputStream: DataOutputStream
    private lateinit var clientThread: Thread

    private val gson = Gson()

    private const val CONNECTION = "connection"
    private const val DIAGNOSTIC = "diagnostic"
    private const val CONFIGURE = "configure"
    private const val SYNC = "sync"
    private const val PREFS = "prefs"

    fun connect(onReceiveResponse: OnReceiveResponse) {
        val data = gson.toJson(SocketRequest(CONNECTION))
        sendData(data, onReceiveResponse)
    }

    fun getDiagnosticData(onReceiveResponse: OnReceiveResponse) {
        val data = gson.toJson(SocketRequest(DIAGNOSTIC))
        sendData(data, onReceiveResponse)
    }

    fun getCurrentConfiguration(onReceiveResponse: OnReceiveResponse) {
        val data = gson.toJson(SocketRequest(CONFIGURE))
        sendData(data, onReceiveResponse)
    }

    fun syncConfiguration(config: List<String>, onReceiveResponse: OnReceiveResponse) {
        val jsonString = gson.toJson(SyncConfigurationRequest(SyncConfiguration(config)))
        sendData(jsonString, onReceiveResponse)
    }

    fun getAllCurrentPrefs(onReceiveResponse: OnReceiveResponse) {
        val data = gson.toJson(SocketRequest(PREFS))
        sendData(data, onReceiveResponse)
    }

    private fun sendData(data: String, onReceiveResponse: OnReceiveResponse) {
        clientThread = Thread(Runnable {
            try {
                socket = Socket("192.168.43.1", 9999)

                while (true) {
                    outputStream = DataOutputStream(socket.getOutputStream())

                    outputStream.writeUTF(data)
                    outputStream.flush()

                    //receiving data
                    val dataInput = DataInputStream(socket.getInputStream()).readUTF()
                    if (!dataInput.isNullOrBlank()) {

                        val receiveJson = JSONObject(dataInput)
                        val jsonIterator = receiveJson.keys()

                        val keys = jsonIterator.asSequence().toList()
                        when (keys[0].toString()) {
                            CONFIGURE -> {
                                val response =
                                    gson.fromJson(dataInput, ConfigurationResponse::class.java)
                                onReceiveResponse.onReceive(response)
                            }
                            DIAGNOSTIC -> {
                                val response =
                                    gson.fromJson(dataInput, DiagnosticResponse::class.java)
                                onReceiveResponse.onReceive(response)
                            }
                            CONNECTION -> {
                                val response =
                                    gson.fromJson(dataInput, ConnectionResponse::class.java)
                                onReceiveResponse.onReceive(response)
                            }
                            SYNC -> {
                                val response =
                                    gson.fromJson(dataInput, SyncConfigurationResponse::class.java)
                                if (response.sync.status == "success") {
                                    onReceiveResponse.onReceive(response)
                                } else {
                                    onReceiveResponse.onFailed("Sync failed, there is something wrong on the server")
                                }
                            }
                            PREFS -> {
                                val response = gson.fromJson(dataInput, PrefsResponse::class.java)
                                onReceiveResponse.onReceive(response)
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                if (e is ConnectException) {
                    onReceiveResponse.onFailed("failed to connect to the server")
                }
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
    fun onFailed(message: String)
}
