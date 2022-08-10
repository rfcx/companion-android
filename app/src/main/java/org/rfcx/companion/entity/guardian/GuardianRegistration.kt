package org.rfcx.companion.entity.guardian

import io.realm.RealmModel
import io.realm.annotations.PrimaryKey
import org.rfcx.companion.entity.request.GuardianRegisterRequest
import org.rfcx.companion.entity.response.GuardianRegisterResponse
import java.security.SecureRandom

data class GuardianRegistration(
    @PrimaryKey
    var guid: String = "",
    var token: String = "",
    var keystorePassphrase: String = "L2Cevkmc9W5fFCKn",
    var pinCode: String = "",
    var apiMqttHost: String = "staging-api-mqtt.rfcx.org",
    var apiSmsAddress: String = "+14154803657",
    var env: String = "staging"
) : RealmModel {

    fun toSocketFormat(): GuardianRegisterResponse {
        return GuardianRegisterResponse(guid, token, keystorePassphrase, pinCode, apiMqttHost, apiSmsAddress)
    }

    fun toRequest(): GuardianRegisterRequest {
        return GuardianRegisterRequest(guid, token, pinCode)
    }

    companion object {
        const val TABLE_NAME = "GuardianRegistration"
        const val FIELD_GUID = "guid"
        const val FIELD_TOKEN = "token"
        const val FIELD_KEYSTORE_PASSPHRASE = "keystorePassphrase"
        const val FIELD_PIN_CODE = "pinCode"
        const val FIELD_API_MQTT_HOST = "apiMqttHost"
        const val FIELD_API_SMS_ADDRESS = "apiSmsAddress"
        const val FIELD_ENV = "env"
    }
}

fun generateSecureRandomHash(length: Int): String {
    val allAllowed = "abcdefghijklmnopqrstuvwxyzABCDEFGJKLMNPRSTUVWXYZ0123456789".toCharArray()
    val random = SecureRandom()
    val stringBuilder = StringBuilder()
    for (i in 0 until length) {
        stringBuilder.append(allAllowed[random.nextInt(allAllowed.size)])
    }
    return stringBuilder.toString()
}
