package org.rfcx.companion.connection.socket

import android.os.SystemClock
import androidx.lifecycle.MutableLiveData
import com.google.gson.Gson
import com.google.gson.JsonObject
import org.rfcx.companion.entity.guardian.Classifier
import org.rfcx.companion.util.file.APKUtils
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.File
import java.net.Socket
import kotlin.math.roundToInt

object FileSocketManager {

    private var socket: Socket? = null
    private var outputStream: DataOutputStream? = null
    private var inputStream: DataInputStream? = null
    private var clientThread: Thread? = null // Thread for socket communication

    private lateinit var inComingMessageThread: Thread

    val pingBlob = MutableLiveData<JsonObject>()
    val uploadingProgress = MutableLiveData<Int>()

    fun sendFile(filePath: String, meta: String? = null) {
        sendMessage(APKUtils.getAPKFileFromPath(filePath), meta)
    }

    fun sendFile(classifier: Classifier) {
        sendMessage(APKUtils.getAPKFileFromPath(classifier.path), classifier.toGuardianClassifier())
    }

    private fun sendMessage(file: File, meta: String?) {
        clientThread = Thread {
            try {
                socket = Socket("192.168.43.1", 9996)
                socket?.keepAlive = true
                startInComingMessageThread()
                outputStream = DataOutputStream(socket?.getOutputStream())

                val fileSize = file.length()
                var progress = 0
                val buffer = ByteArray(8192)
                var count: Int
                val inp = file.inputStream()
                outputStream?.write(file.name.toByteArray())
                outputStream?.write("|".toByteArray())

                if (meta != null) {
                    outputStream?.write(meta.toByteArray())
                }

                outputStream?.write("|".toByteArray())
                while (true) {
                    count = inp.read(buffer)
                    progress += count
                    if (count < 0) {
                        break
                    }
                    outputStream?.write(buffer, 0, count)
                    uploadingProgress.postValue(((progress.toDouble() / fileSize.toDouble()) * 100).roundToInt())
                }

                outputStream?.flush()

                SystemClock.sleep(1000)

                outputStream?.write("****".toByteArray())
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
                        val ping = Gson().fromJson(dataInput, JsonObject::class.java)
                        pingBlob.postValue(ping)
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

        outputStream?.close()

        inputStream?.close()

        outputStream?.close()
        socket?.close()
    }
}
