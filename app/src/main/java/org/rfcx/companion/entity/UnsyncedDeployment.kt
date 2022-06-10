package org.rfcx.companion.entity

import java.util.*

data class UnsyncedDeployment(
    val id: Int,
    val name: String,
    val deployedAt: Date,
    val error: String?
)
