package org.rfcx.companion.util

import java.security.SecureRandom

fun generateSecureRandomHash(length: Int): String {
    val random = SecureRandom()
    val bytes = ByteArray(length)
    random.nextBytes(bytes)
    return bytes.contentToString()
}
