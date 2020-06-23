package org.rfcx.audiomoth.entity.socket

data class DiagnosticResponse(
    val diagnostic: Diagnostic
): SocketResposne

data class Diagnostic(
    val battery: Int,
    val total_record: Int,
    val total_size: Int
)
