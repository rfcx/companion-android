package org.rfcx.audiomoth.entity

enum class SyncState(val key: Int) {
    Unsent(0), Uploading(1), Uploaded(2)
}