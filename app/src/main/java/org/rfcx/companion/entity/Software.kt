package org.rfcx.companion.entity

data class Software(
    val name: GuardianSoftware,
    val version: String,
    val path: String
)

enum class GuardianSoftware(val value: String) {
    ADMIN("admin"), CLASSIFY("classify"), GUARDIAN("guardian"), UPDATER(
        "updater"
    )
}
