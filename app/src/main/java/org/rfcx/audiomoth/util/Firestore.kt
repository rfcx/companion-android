package org.rfcx.audiomoth.util

import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import org.rfcx.audiomoth.entity.Device
import java.lang.Exception


interface FirestoreCallback {
    fun onCompleteListener()
    fun addOnFailureListener(exception: Exception)
}

interface FirestoreResponseCallback<T> {
    fun onSuccessListener(response: T)
    fun addOnFailureListener(exception: Exception)
}

class Firestore {
    val db = Firebase.firestore

    fun saveDevice(device: Device, callback: FirestoreCallback) {
        db.collection(COLLECTION_DEVICES).document().set(device)
            .addOnCompleteListener {
                callback.onCompleteListener()
            }.addOnFailureListener {
                callback.addOnFailureListener(it)
            }
    }

    fun getDeviceById(deviceId: String, callback: FirestoreResponseCallback<Device?>) {
        db.collection(COLLECTION_DEVICES).whereEqualTo(Device.FIELD_DEVICE_ID, deviceId).limit(1)
            .get()
            .addOnSuccessListener { querySnapshot ->
                val documents = querySnapshot.documents
                if (documents.isNotEmpty()) {
                    val devices = documents.map { it.toObject(Device::class.java) }
                    callback.onSuccessListener(devices[0])
                } else {
                    callback.onSuccessListener(null)
                }
            }
            .addOnFailureListener {
                callback.addOnFailureListener(it)
            }
    }

    companion object {
        // Firestore Collection
        const val COLLECTION_DEVICES = "devices"
    }
}
