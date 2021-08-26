package org.rfcx.companion.entity.socket.request

import kotlin.random.Random

data class InstructionMessage(
    val instructions: List<Instruction>
) {
    companion object {
        fun toMessage(
            type: InstructionType,
            cmd: InstructionCommand,
            meta: String
        ): InstructionMessage {
            val instruction = Instruction(
                type = type.value,
                cmd = cmd.value,
                meta = meta
            )
            return InstructionMessage(listOf(instruction))
        }
    }
}

data class Instruction(
    val id: Int = Random.nextInt(1, 100),
    val type: String,
    val cmd: String,
    val at: String = "",
    val meta: String = "{}"
)

enum class InstructionType(val value: String) {
    SET("set"),
    CTRL("ctrl"),
    SEND("send")
}

enum class InstructionCommand(val value: String) {
    PREFS("prefs"),
    WIFI("wifi"),
    PING("ping"),
    IDENTITY("identity"),
    SITE("site")
}

