package org.rfcx.companion.repo

interface ResponseCallback<T> {
    fun onSuccessCallback(response: T)
    fun onFailureCallback(errorMessage: String?)
}
