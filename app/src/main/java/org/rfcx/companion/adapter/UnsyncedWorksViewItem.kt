package org.rfcx.companion.adapter

import java.util.*

sealed class UnsyncedWorksViewItem {
    data class Deployment(val id: Int, val name: String, val deployedAt: Date, val error: String?) : UnsyncedWorksViewItem()
    data class Header(val name: String) : UnsyncedWorksViewItem()
}
