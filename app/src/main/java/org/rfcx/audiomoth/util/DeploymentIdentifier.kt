package org.rfcx.audiomoth.util

data class DeploymentIdentifier(val hex: String) {

    val isValid = hex.length <= 16 && hex.replace("[0-9A-F]*".toRegex(), "").isEmpty()

    val toByteArray: ByteArray get() =
        ByteArray(hex.length / 2) { hex.substring(it * 2, it * 2 + 2).toInt(16).toByte() }
}
