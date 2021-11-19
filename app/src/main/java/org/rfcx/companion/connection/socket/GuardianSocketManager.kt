package org.rfcx.companion.connection.socket

import android.content.Context
import android.util.Log
import androidx.lifecycle.MutableLiveData
import com.google.gson.Gson
import com.google.gson.JsonObject
import org.json.JSONObject
import org.rfcx.companion.entity.response.GuardianRegisterResponse
import org.rfcx.companion.entity.socket.request.*
import org.rfcx.companion.entity.socket.response.*
import org.rfcx.companion.util.MicrophoneTestUtils
import org.rfcx.companion.util.Preferences
import java.io.DataInputStream
import java.io.DataOutputStream
import java.net.Socket

object GuardianSocketManager {

    private var socket: Socket? = null
    private var outputStream: DataOutputStream? = null
    private var inputStream: DataInputStream? = null
    private var clientThread: Thread? = null // Thread for socket communication

    private lateinit var inComingMessageThread: Thread

    private val gson = Gson()

    // List of socket commands
    private const val CONNECTION = "connection"
    private const val DIAGNOSTIC = "diagnostic"
    private const val CONFIGURE = "configure"
    private const val SYNC = "sync"
    private const val PREFS = "prefs"
    private const val SIGNAL = "signal"
    private const val SIGNAL_INFO = "signal_info"
    private const val MICROPHONE_TEST = "microphone_test"
    private const val CHECKIN = "checkin"
    private const val SENTINEL = "sentinel"
    private const val REGISTER = "register"
    private const val IS_REGISTERED = "is_registered"
    private const val STOP_WIFI = "stop_wifi"
    private const val IS_RECORDING = "is_recording"

    private var audioChunks = arrayListOf<String>()
    private var microphoneTestUtils: MicrophoneTestUtils? = null
    private var tempAudio = ByteArray(0)
    private var isTestingFirstTime = true


    val connection = MutableLiveData<ConnectionResponse>()
    val syncConfiguration = MutableLiveData<SyncConfigurationResponse>()
    val prefs = MutableLiveData<PrefsResponse>()
    val signal = MutableLiveData<SignalResponse>()
    val liveAudio = MutableLiveData<MicrophoneTestResponse>()
    val spectrogram = MutableLiveData<ByteArray>()
    val checkInTest = MutableLiveData<CheckInTestResponse>()
    val sentinel = MutableLiveData<SentinelResponse>()
    val register = MutableLiveData<RegisterResponse>()
    val isRegistered = MutableLiveData<CheckGuardianRegistered>()
    val recorderState = MutableLiveData<RecorderStateResponse>()

    val pingBlob = MutableLiveData<GuardianPing>()

    init {
        connection.postValue(ConnectionResponse())
        syncConfiguration.postValue(SyncConfigurationResponse())
        prefs.postValue(PrefsResponse())
        signal.postValue(SignalResponse())
        liveAudio.postValue(MicrophoneTestResponse())
        spectrogram.postValue(ByteArray(2))
        checkInTest.postValue(CheckInTestResponse())
        sentinel.postValue(SentinelResponse())
        register.postValue(RegisterResponse())
        isRegistered.postValue(CheckGuardianRegistered())
        recorderState.postValue(RecorderStateResponse())
    }

    fun getConnection() {
        val data = gson.toJson(
            SocketRequest(
                CONNECTION
            )
        )
        sendMessage(data)
    }

    fun getDiagnosticData() {
        val data = gson.toJson(
            SocketRequest(
                DIAGNOSTIC
            )
        )
        sendMessage(data)
    }

    fun getCurrentConfiguration() {
        val data = gson.toJson(
            SocketRequest(
                CONFIGURE
            )
        )
        sendMessage(data)
    }

    fun syncConfiguration(config: String) {
        sendInstructionMessage(InstructionType.SET, InstructionCommand.PREFS, config)
    }

    fun getCheckInTest(command: CheckinCommand) {
        val jsonString = gson.toJson(
            CheckinRequest(
                Checkin(
                    CheckinInfo(command.value)
                )
            )
        )
        sendMessage(jsonString)
    }

    fun sendGuardianRegistration(response: GuardianRegisterResponse) {
        val renamedJson = JsonObject()
        renamedJson.addProperty("guid", response.guid)
        renamedJson.addProperty("token", response.token)
        renamedJson.addProperty("keystore_passphrase", response.keystorePassphrase)
        renamedJson.addProperty("pin_code", response.pinCode)
        renamedJson.addProperty("api_mqtt_host", response.apiMqttHost)
        renamedJson.addProperty("api_sms_address", response.apiSmsAddress)
        sendInstructionMessage(InstructionType.SET, InstructionCommand.IDENTITY, gson.toJson(renamedJson))
    }

    fun stopGuardianWiFi() {
        sendInstructionMessage(InstructionType.CTRL, InstructionCommand.WIFI)
    }

    fun sendInstructionMessage(type: InstructionType, command: InstructionCommand, meta: String = "{}") {
        val data = gson.toJson(InstructionMessage.toMessage(type, command, meta))
        sendMessage(data)
    }

    fun resetPrefsValue() {
        this.prefs.value = PrefsResponse()
    }

    fun resetRegisterResult() {
        this.register.value = RegisterResponse()
    }

    fun resetAllValuesToDefault() {
        connection.value =
            ConnectionResponse()
        syncConfiguration.value =
            SyncConfigurationResponse()
        prefs.value = PrefsResponse()
        signal.value = SignalResponse()
        liveAudio.value =
            MicrophoneTestResponse()
        spectrogram.value = ByteArray(2)
        checkInTest.value =
            CheckInTestResponse()
        sentinel.value =
            SentinelResponse()
        register.value = RegisterResponse()
        isRegistered.value = CheckGuardianRegistered()
        recorderState.value = RecorderStateResponse()
    }

    private fun sendMessage(message: String) {
        clientThread = Thread {
            try {
                socket = Socket("192.168.43.1", 9999)
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

                        val receiveJson = JSONObject(dataInput)
                        val jsonIterator = receiveJson.keys()

                        Log.d("LOG", dataInput)
                        val ping = gson.fromJson(dataInput, GuardianPing::class.java)
                        pingBlob.postValue(ping)

                        val keys = jsonIterator.asSequence().toList()
                        when (keys[0].toString()) {
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
                                audioChunks.add(response.audioBuffer.buffer)
                                if (response.audioBuffer.amount == response.audioBuffer.number) {
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
                                    this.liveAudio.postValue(response)
                                }
                            }
                            CHECKIN -> {
                                val response =
                                    gson.fromJson(dataInput, CheckInTestResponse::class.java)
                                this.checkInTest.postValue(response)
                            }
                            SENTINEL -> {
                                val response =
                                    gson.fromJson(dataInput, SentinelResponse::class.java)
                                this.sentinel.postValue(response)
                            }
                            REGISTER -> {
                                val response =
                                    gson.fromJson(dataInput, RegisterResponse::class.java)
                                this.register.postValue(response)
                            }
                            IS_REGISTERED -> {
                                val response =
                                    gson.fromJson(dataInput, CheckGuardianRegistered::class.java)
                                this.isRegistered.postValue(response)
                            }
                            IS_RECORDING -> {
                                val response =
                                    gson.fromJson(dataInput, RecorderStateResponse::class.java)
                                this.recorderState.postValue(response)
                            }
                        }
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
