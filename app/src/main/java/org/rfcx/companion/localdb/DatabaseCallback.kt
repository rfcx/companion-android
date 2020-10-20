package org.rfcx.companion.localdb

interface DatabaseCallback {
    fun onSuccess()
    fun onFailure(errorMessage: String)
}
