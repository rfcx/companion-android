package org.rfcx.companion.entity

enum class SyncState(val key: Int) {
    Unsent(0), Sending(1), Sent(2)
}
