package org.rfcx.companion.entity.socket.request

data class InstructionMessage(
    val type: InstructionType,
    val command: InstructionCommand,
    val meta: String?
)

enum class InstructionType(val value: String) {
    SET("set"),
    CTRL("ctrl"),
    SEND("send")
}

enum class InstructionCommand(val value: String) {
    PREFS("prefs"),
    WIFI("wifi"),
    PING("ping")
}

