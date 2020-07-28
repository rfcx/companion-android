package org.rfcx.audiomoth.connection.socket

import com.google.gson.Gson
import org.json.JSONObject
import org.rfcx.audiomoth.entity.socket.*
import org.rfcx.audiomoth.util.MicrophoneTestUtils
import java.io.DataInputStream
import java.io.DataOutputStream
import java.net.ConnectException
import java.net.Socket
import org.json.JSONObject
import org.rfcx.audiomoth.entity.socket.*

object SocketManager {

    private lateinit var socket: Socket
    private lateinit var outputStream: DataOutputStream
    private lateinit var clientThread: Thread // Thread for socket communication
    private lateinit var audioThread: Thread // Separated thread for queuing audio and set audio track

    private val gson = Gson()

    private const val CONNECTION = "connection"
    private const val DIAGNOSTIC = "diagnostic"
    private const val CONFIGURE = "configure"
    private const val SYNC = "sync"
    private const val PREFS = "prefs"
    private const val SIGNAL = "signal"
    private const val SIGNAL_INFO = "signal_info"
    private const val MICROPHONE_TEST = "microphone_test"

    private val audioQueue = arrayListOf<String>()
    private var microphoneTestUtils: MicrophoneTestUtils? = null
    private var isTestingFirstTime = true

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

    fun getSignalStrength(onReceiveResponse: OnReceiveResponse) {
        val data = gson.toJson(SocketRequest(SIGNAL))
        sendData(data, onReceiveResponse)
    }

    fun getLiveAudioBuffer(
        micTestUtils: MicrophoneTestUtils,
        onReceiveResponse: OnReceiveResponse
    ) {
        val data = gson.toJson(SocketRequest(MICROPHONE_TEST))
        sendData(data, onReceiveResponse, micTestUtils)
    }

    private fun sendData(
        data: String,
        onReceiveResponse: OnReceiveResponse,
        micTestUtils: MicrophoneTestUtils? = null
    ) {
        microphoneTestUtils = micTestUtils
        clientThread = Thread(Runnable {
            try {
                socket = Socket("192.168.43.1", 9999)

                outputStream = DataOutputStream(socket.getOutputStream())

                outputStream.writeUTF(data)
                outputStream.flush()

                while (true) {
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
                                    gson.fromJson(
                                        dataInput,
                                        SyncConfigurationResponse::class.java
                                    )
                                if (response.sync.status == "success") {
                                    onReceiveResponse.onReceive(response)
                                } else {
                                    onReceiveResponse.onFailed("Sync failed, there is something wrong on the server")
                                }
                            }
                            PREFS -> {
                                val response =
                                    gson.fromJson(dataInput, PrefsResponse::class.java)
                                onReceiveResponse.onReceive(response)
                            }
                            SIGNAL_INFO -> {
                                val response =
                                    gson.fromJson(dataInput, SignalResponse::class.java)
                                onReceiveResponse.onReceive(response)
                            }
                            MICROPHONE_TEST -> {
                                val response =
                                    gson.fromJson(dataInput, MicrophoneTestResponse::class.java)
                                if (isTestingFirstTime) {
                                    microphoneTestUtils?.let { util ->
                                        util.init(util.getEncodedAudioBufferSize(response.audioBuffer.buffer))
                                        util.play()
                                    }
                                    setAudioFromQueue()
                                    isTestingFirstTime = false
                                }
                                audioQueue.add(response.audioBuffer.buffer)
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

        if (!clientThread.isAlive){
            clientThread.start()
        }
    }

    private fun setAudioFromQueue() {
        audioThread = Thread(Runnable {
            while (!audioThread.isInterrupted) {
                try {
                    if (audioQueue.isNotEmpty()) {
                        val audio = audioQueue[0]
                        microphoneTestUtils?.apply {
                            buffer = decodeEncodedAudio(audio)
                        }.also { util ->
                            util?.setTrack()
                        }
                        audioQueue.remove(audio)
                    }
                } catch (e: InterruptedException) {
                    audioThread.interrupt()
                } catch (e: IllegalStateException) {
                    e.printStackTrace()
                }
            }
        })

        audioThread.start()
    }

    fun stopConnection() {
        clientThread.interrupt()
    }

    fun stopAudioQueueThread() {
        if (::audioThread.isInitialized){
            audioThread.interrupt()
        }
    }
}

interface OnReceiveResponse {
    fun onReceive(response: SocketResposne)
    fun onFailed(message: String)
}
