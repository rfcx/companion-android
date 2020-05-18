package org.rfcx.audiomoth.entity

import java.io.Serializable

open class User(
    val name: String
) : Serializable {
    companion object {
        const val FIELD_NAME = "name"
    }
}
