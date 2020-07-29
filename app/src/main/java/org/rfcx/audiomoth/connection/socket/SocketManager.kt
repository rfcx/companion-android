package org.rfcx.audiomoth.connection.socket

import android.util.Log
import androidx.lifecycle.MutableLiveData
import com.google.gson.Gson
import org.json.JSONObject
import org.rfcx.audiomoth.entity.socket.*
import org.rfcx.audiomoth.util.MicrophoneTestUtils
import java.io.DataInputStream
import java.io.DataOutputStream
import java.net.Socket

object SocketManager {

    private var socket: Socket? = null
    private var outputStream: DataOutputStream? = null
    private var inputStream: DataInputStream? = null
    private var clientThread: Thread? = null// Thread for socket communication
    private var audioThread: Thread? = null// Separated thread for queuing audio and set audio track

    private lateinit var inComingMessageThread: Thread

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

    val connection = MutableLiveData<ConnectionResponse>()
    val diagnostic = MutableLiveData<DiagnosticResponse>()
    val currentConfiguration = MutableLiveData<ConfigurationResponse>()
    val syncConfiguration = MutableLiveData<SyncConfigurationResponse>()
    val prefs = MutableLiveData<PrefsResponse>()
    val signal = MutableLiveData<SignalResponse>()
    val liveAudio = MutableLiveData<MicrophoneTestResponse>()

    init {
        connection.value = ConnectionResponse()
        diagnostic.value = DiagnosticResponse()
        currentConfiguration.value = ConfigurationResponse()
        syncConfiguration.value = SyncConfigurationResponse()
        prefs.value = PrefsResponse()
        signal.value = SignalResponse()
        liveAudio.value = MicrophoneTestResponse()
    }

    fun getConnection() {
        val data = gson.toJson(SocketRequest(CONNECTION))
        sendMessage(data)
    }

    fun getDiagnosticData() {
        val data = gson.toJson(SocketRequest(DIAGNOSTIC))
        sendMessage(data)
    }

    fun getCurrentConfiguration() {
        val data = gson.toJson(SocketRequest(CONFIGURE))
        sendMessage(data)
    }

    fun syncConfiguration(config: List<String>) {
        val jsonString = gson.toJson(SyncConfigurationRequest(SyncConfiguration(config)))
        sendMessage(jsonString)
    }

    fun getSignalStrength() {
        val data = gson.toJson(SocketRequest(SIGNAL))
        sendMessage(data)
    }

    fun getLiveAudioBuffer(micTestUtils: MicrophoneTestUtils) {
        this.microphoneTestUtils = micTestUtils
        val data = gson.toJson(SocketRequest(MICROPHONE_TEST))
        sendMessage(data)
    }

    private fun sendMessage(message: String) {
        clientThread = Thread(Runnable {
            try {
                Log.d("Socket", "Connecting thread")
                socket = Socket("192.168.43.1", 9999)
                startInComingMessageThread()
                Log.d("Socket", "Sending message $message")
                outputStream = DataOutputStream(socket?.getOutputStream())
                outputStream?.writeUTF(message)
                outputStream?.flush()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        })
        clientThread?.start()
    }

    private fun startInComingMessageThread() {
        inComingMessageThread = Thread(Runnable {
            try {
                while (true) {
                    inputStream = DataInputStream(socket!!.getInputStream())
                    val dataInput = inputStream?.readUTF()
                    if (!dataInput.isNullOrBlank()) {

                        Log.d("Socket", "getting new command")
                        val receiveJson = JSONObject(dataInput)
                        val jsonIterator = receiveJson.keys()

                        val keys = jsonIterator.asSequence().toList()
                        when (keys[0].toString()) {
                            CONFIGURE -> {
                                val response =
                                    gson.fromJson(dataInput, ConfigurationResponse::class.java)
                                this.currentConfiguration.postValue(response)
                            }
                            DIAGNOSTIC -> {
                                val response =
                                    gson.fromJson(dataInput, DiagnosticResponse::class.java)
                                this.diagnostic.postValue(response)
                            }
                            CONNECTION -> {
                                val response =
                                    gson.fromJson(dataInput, ConnectionResponse::class.java)
                                this.connection.postValue(response)
                            }
                            SYNC -> {
                                val response =
                                    gson.fromJson(
                                        dataInput,
                                        SyncConfigurationResponse::class.java
                                    )
                                if (response.sync.status == Status.SUCCESS.value) {
                                    this.syncConfiguration.postValue(response)
                                } else {
                                    this.syncConfiguration.postValue(response)
                                }
                            }
                            PREFS -> {
                                val response =
                                    gson.fromJson(dataInput, PrefsResponse::class.java)
                                this.prefs.postValue(response)
                            }
                            SIGNAL_INFO -> {
                                val response =
                                    gson.fromJson(dataInput, SignalResponse::class.java)
                                this.signal.postValue(response)
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
                                this.liveAudio.postValue(response)
                                audioQueue.add(response.audioBuffer.buffer)
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        })
        inComingMessageThread.start()
    }

    private fun setAudioFromQueue() {
        audioThread = Thread(Runnable {
            while (!audioThread!!.isInterrupted) {
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
                    audioThread?.interrupt()
                } catch (e: IllegalStateException) {
                    e.printStackTrace()
                }
            }
        })

        audioThread?.start()
    }

    fun stopConnection() {
        //stop incoming message thread
        inComingMessageThread.interrupt()

        //stop server thread
        clientThread?.interrupt()

        outputStream?.close()

        inputStream?.close()

        outputStream?.close()
        socket?.close()
    }

    fun stopAudioQueueThread() {
        audioThread?.interrupt()
    }
}
