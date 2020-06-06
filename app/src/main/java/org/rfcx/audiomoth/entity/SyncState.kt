package org.rfcx.audiomoth.entity

enum class SyncState(key: Int) {
    unsent(0), uploading(1), uploaded(2)
}