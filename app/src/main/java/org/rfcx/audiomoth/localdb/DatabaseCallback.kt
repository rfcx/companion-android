package org.rfcx.audiomoth.localdb

interface DatabaseCallback {
    fun onSuccess()
    fun onFailure(errorMessage: String)
}