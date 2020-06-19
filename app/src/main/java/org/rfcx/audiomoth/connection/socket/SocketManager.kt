package org.rfcx.audiomoth.connection.socket

import android.app.Activity
import android.content.Context
import android.util.Log
import com.google.gson.Gson
import org.json.JSONObject
import org.rfcx.audiomoth.entity.guardian.GuardianConfiguration
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

    private val CONNECTION = "connection"
    private val DIAGNOSTIC = "diagnostic"
    private val CONFIGURE = "configure"
    private val SYNC = "sync"

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

    fun syncConfiguration(config: GuardianConfiguration, onReceiveResponse: OnReceiveResponse) {
        val jsonString = gson.toJson(config)
        sendData(jsonString, onReceiveResponse)
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

                        Log.d(LOGTAG, "Receiving data from Server: $dataInput")

                        val receiveJson = JSONObject(dataInput)
                        val jsonIterator = receiveJson.keys()

                        val keys = jsonIterator.asSequence().toList()
                        when (keys[0].toString()) {
                            CONFIGURE -> {
                                val response = gson.fromJson(dataInput, ConfigurationResponse::class.java)
                                Log.d(LOGTAG, "Configure response: $response")
                                onReceiveResponse.onReceive(response)
                            }
                            DIAGNOSTIC -> {
                                val response = gson.fromJson(dataInput, DiagnosticResponse::class.java)
                                Log.d(LOGTAG, "Diagnostic response: ${response.diagnostic}")
                                onReceiveResponse.onReceive(response)
                            }
                            CONNECTION -> {
                                val response = gson.fromJson(dataInput, ConnectionResponse::class.java)
                                Log.d(LOGTAG, "Connection status: ${response.connection.status}")
                                onReceiveResponse.onReceive(response)
                            }
                            SYNC -> {
                                val response = gson.fromJson(dataInput, SyncConfigurationResponse::class.java)
                                Log.d(LOGTAG, "Sync status: ${response.sync.status}")
                                onReceiveResponse.onReceive(response)
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
    fun onFailed()
}
