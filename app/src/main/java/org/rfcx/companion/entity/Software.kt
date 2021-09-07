package org.rfcx.companion.entity

data class Software(
    val appName: GuardianSoftware,
    val apkVersion: String
)

enum class GuardianSoftware(val value: String) {
    ADMIN("admin"), CLASSIFY("classify"), GUARDIAN("guardian"), UPDATER(
        "updater"
    )
}
