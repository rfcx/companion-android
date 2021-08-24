package org.rfcx.companion.connection.socket

import androidx.lifecycle.MutableLiveData
import com.google.gson.Gson
import org.rfcx.companion.entity.socket.response.AudioCastPing
import org.rfcx.companion.util.MicrophoneTestUtils
import java.io.DataInputStream
import java.io.DataOutputStream
import java.net.Socket

object AudioCastSocketManager {

    private var socket: Socket? = null
    private var outputStream: DataOutputStream? = null
    private var inputStream: DataInputStream? = null
    private var clientThread: Thread? = null // Thread for socket communication

    private lateinit var inComingMessageThread: Thread

    private val gson = Gson()

    private var audioChunks = arrayListOf<String>()
    private var microphoneTestUtils: MicrophoneTestUtils? = null
    private var tempAudio = ByteArray(0)
    private var isTestingFirstTime = true

    val pingBlob = MutableLiveData<AudioCastPing>()
    val spectrogram = MutableLiveData<ByteArray>()

    fun resetAllValuesToDefault() {
        pingBlob.value = AudioCastPing()
        spectrogram.value = ByteArray(2)
    }

    //just to connect to server
    fun connect(micTestUtils: MicrophoneTestUtils) {
        this.microphoneTestUtils = micTestUtils
        sendMessage("")
    }

    private fun sendMessage(message: String) {
        clientThread = Thread {
            try {
                socket = Socket("192.168.43.1", 9998)
                socket?.keepAlive = true
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
                    if (!dataInput.isNullOrBlank()) {

                        val ping = gson.fromJson(dataInput, AudioCastPing::class.java)

                        audioChunks.add(ping.buffer)
                        if (ping.amount == ping.number) {
                            var fullAudio = ByteArray(0)

                            audioChunks
                                .map { microphoneTestUtils?.decodeEncodedAudio(it) }
                                .forEach { fullAudio += it!! }

                            if (isTestingFirstTime) {
                                microphoneTestUtils?.let { util ->
                                    util.init(fullAudio.size)
                                    util.play()
                                }
                                isTestingFirstTime = false
                            }
                            if (!tempAudio.contentEquals(fullAudio)) {
                                tempAudio = fullAudio
                                microphoneTestUtils?.let {
                                    it.buffer = fullAudio
                                    it.setTrack()
                                    this.spectrogram.postValue(it.buffer)
                                }
                            }
                            audioChunks.clear()
                            this.pingBlob.postValue(ping)
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        inComingMessageThread.start()
    }

    fun resetMicrophoneDefaultValue() {
        isTestingFirstTime = true
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
