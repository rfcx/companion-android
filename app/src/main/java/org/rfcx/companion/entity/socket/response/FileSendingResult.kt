package org.rfcx.companion.entity.socket.response

data class FileSendingResult(
    val admin: Boolean,
    val classify: Boolean,
    val guardian: Boolean,
    val updater: Boolean
)
