package org.rfcx.companion.util

import java.lang.StringBuilder
import java.security.SecureRandom

fun generateSecureRandomHash(length: Int): String {
    val allAllowed = "abcdefghijklmnopqrstuvwxyzABCDEFGJKLMNPRSTUVWXYZ0123456789".toCharArray()
    val random = SecureRandom()
    val stringBuilder = StringBuilder()
    for (i in 0 until length) {
        stringBuilder.append(allAllowed[random.nextInt(allAllowed.size)])
    }
    return stringBuilder.toString()
}
