package org.rfcx.audiomoth.repo

interface ResponseCallback<T> {
    fun onSuccessCallback(response :T)
    fun onFailureCallback(errorMessage: String?)
}