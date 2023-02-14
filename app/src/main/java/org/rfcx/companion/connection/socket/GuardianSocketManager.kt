package org.rfcx.companion.connection.socket

import androidx.lifecycle.MutableLiveData
import com.google.gson.Gson
import com.google.gson.JsonObject
import org.rfcx.companion.entity.guardian.GuardianRegistration
import org.rfcx.companion.entity.response.GuardianRegisterResponse
import org.rfcx.companion.entity.socket.request.InstructionCommand
import org.rfcx.companion.entity.socket.request.InstructionMessage
import org.rfcx.companion.entity.socket.request.InstructionType
import org.rfcx.companion.entity.socket.request.SocketRequest
import org.rfcx.companion.entity.socket.response.GuardianPing
import org.rfcx.companion.util.socket.PingUtils
import java.io.DataInputStream
import java.io.DataOutputStream
import java.net.Socket
import java.util.*

object GuardianSocketManager {

    private var socket: Socket? = null
    private var outputStream: DataOutputStream? = null
    private var inputStream: DataInputStream? = null
    private var clientThread: Thread? = null // Thread for socket communication

    private lateinit var inComingMessageThread: Thread

    private val gson = Gson()

    // List of socket commands
    private const val CONNECTION = "connection"

    val pingBlob = MutableLiveData<GuardianPing>()

    fun getConnection() {
        val data = gson.toJson(
            SocketRequest(
                CONNECTION
            )
        )
        sendMessage(data)
    }

    fun sendCellOnlyPrefs() {
        val prefs = JsonObject()
        prefs.addProperty("api_satellite_protocol", "off")
        prefs.addProperty("enable_audio_classify", "false")
        prefs.addProperty("enable_checkin_publish", "true")
        prefs.addProperty(
            "api_ping_cycle_fields",
            "checkins,instructions,prefs,sms,meta,detections,purged"
        )
        prefs.addProperty("enable_audio_cast", "true")
        prefs.addProperty("enable_file_socket", "true")
        prefs.addProperty("api_protocol_escalation_order", "mqtt,rest")
        prefs.addProperty("api_satellite_off_hours", "23:55-23:56,23:57-23:59")
        prefs.addProperty("admin_system_timezone", TimeZone.getDefault().id)
        prefs.addProperty("enable_reboot_forced_daily", "true")
        prefs.addProperty("api_ping_cycle_duration", "30")
        prefs.addProperty("api_ping_schedule_off_hours", "23:55-23:56,23:57-23:59")
        syncConfiguration(prefs.toString())
    }

    fun sendCellSMSPrefs() {
        val prefs = JsonObject()
        prefs.addProperty("api_satellite_protocol", "off")
        prefs.addProperty("enable_audio_classify", "true")
        prefs.addProperty("enable_checkin_publish", "true")
        prefs.addProperty(
            "api_ping_cycle_fields",
            "sms,battery,sentinel_power,software,detections,storage,memory,cpu"
        )
        prefs.addProperty("enable_audio_cast", "true")
        prefs.addProperty("enable_file_socket", "true")
        prefs.addProperty("api_protocol_escalation_order", "mqtt,rest,sms")
        prefs.addProperty("api_satellite_off_hours", "23:55-23:56,23:57-23:59")
        prefs.addProperty("admin_system_timezone", TimeZone.getDefault().id)
        prefs.addProperty("enable_reboot_forced_daily", "true")
        prefs.addProperty("api_ping_cycle_duration", "30")
        prefs.addProperty("api_ping_schedule_off_hours", "23:55-23:56,23:57-23:59")
        syncConfiguration(prefs.toString())
    }

    fun sendSatOnlyPrefs(timeOff: String) {
        val prefs = JsonObject()
        prefs.addProperty("api_satellite_protocol", "swm")
        prefs.addProperty("enable_audio_classify", "true")
        prefs.addProperty("enable_checkin_publish", "false")
        prefs.addProperty(
            "api_ping_cycle_fields",
            "battery,sentinel_power,software,swm,detections,storage,memory,cpu"
        )
        prefs.addProperty("enable_audio_cast", "true")
        prefs.addProperty("enable_file_socket", "true")
        prefs.addProperty("api_protocol_escalation_order", "sat")
        prefs.addProperty("api_satellite_off_hours", timeOff)
        prefs.addProperty("admin_system_timezone", TimeZone.getDefault().id)
        prefs.addProperty("enable_reboot_forced_daily", "true")
        prefs.addProperty("api_ping_cycle_duration", "180")
        prefs.addProperty("api_ping_schedule_off_hours", "23:55-23:56,23:57-23:59")
        syncConfiguration(prefs.toString())
    }

    fun sendOfflineModePrefs() {
        val prefs = JsonObject()
        prefs.addProperty("api_satellite_protocol", "off")
        prefs.addProperty("enable_audio_classify", "false")
        prefs.addProperty("enable_checkin_publish", "false")
        prefs.addProperty(
            "api_ping_cycle_fields",
            "checkins,instructions,prefs,sms,meta,detections,purged"
        )
        prefs.addProperty("enable_audio_cast", "true")
        prefs.addProperty("enable_file_socket", "true")
        prefs.addProperty("api_protocol_escalation_order", "")
        prefs.addProperty("api_satellite_off_hours", "23:55-23:56,23:57-23:59")
        prefs.addProperty("admin_system_timezone", TimeZone.getDefault().id)
        prefs.addProperty("enable_reboot_forced_daily", "true")
        prefs.addProperty("api_ping_cycle_duration", "30")
        prefs.addProperty("api_ping_schedule_off_hours", "00:00-23:59")
        syncConfiguration(prefs.toString())
    }

    fun syncConfiguration(config: String) {
        sendInstructionMessage(InstructionType.SET, InstructionCommand.PREFS, config)
    }

    fun sendGuardianRegistration(response: GuardianRegisterResponse) {
        val renamedJson = JsonObject()
        renamedJson.addProperty("guid", response.guid)
        renamedJson.addProperty("token", response.token)
        renamedJson.addProperty("keystore_passphrase", response.keystorePassphrase)
        renamedJson.addProperty("pin_code", response.pinCode)
        renamedJson.addProperty("api_mqtt_host", response.apiMqttHost)
        renamedJson.addProperty("api_sms_address", response.apiSmsAddress)
        sendInstructionMessage(
            InstructionType.SET,
            InstructionCommand.IDENTITY,
            gson.toJson(renamedJson)
        )
    }

    fun sendGuardianRegistration(registration: GuardianRegistration) {
        sendGuardianRegistration(registration.toSocketFormat())
    }

    fun runSpeedTest() {
        sendInstructionMessage(InstructionType.CTRL, InstructionCommand.SPEED_TEST)
    }

    fun restartService(svc: String) {
        val json = JsonObject()
        json.addProperty("service", svc)
        sendInstructionMessage(InstructionType.CTRL, InstructionCommand.RESTART, gson.toJson(json))
    }

    fun sendInstructionMessage(
        type: InstructionType,
        command: InstructionCommand,
        meta: String = "{}"
    ) {
        val data = gson.toJson(InstructionMessage.toMessage(type, command, meta))
        sendMessage(data)
    }

    private fun sendMessage(message: String) {
        clientThread = Thread {
            try {
                socket = Socket("192.168.43.1", 9999)
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
                        val ping = gson.fromJson(message, GuardianPing::class.java)
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
        pingBlob.value = GuardianPing()
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
