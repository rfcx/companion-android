package org.rfcx.audiomoth.util

import android.util.Log
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import org.rfcx.audiomoth.entity.Deployment
import org.rfcx.audiomoth.entity.Locate
import org.rfcx.audiomoth.entity.Profile

interface FirestoreResponseCallback<T> {
    fun onSuccessListener(response: T)
    fun addOnFailureListener(exception: Exception)
}

class Firestore {
    val db = Firebase.firestore
    /* TODO: update get user */
    private val userDocument = db.collection(COLLECTION_USERS).document(USER_ID)

    fun getDeployments(callback: FirestoreResponseCallback<List<Deployment?>>) {
        userDocument.collection(COLLECTION_DEPLOYMENTS).get()
            .addOnSuccessListener { querySnapshot ->
                val documents = querySnapshot.documents
                val response = if (documents.isNotEmpty()) {
                    documents.map { it.toObject(Deployment::class.java) }
                } else {
                    arrayListOf()
                }
                callback.onSuccessListener(response)
            }
            .addOnFailureListener {
                callback.addOnFailureListener(it)
            }
    }

    fun getLocations(callback: FirestoreResponseCallback<List<Locate?>>) {
        userDocument.collection(COLLECTION_LOCATIONS).get()
            .addOnSuccessListener { querySnapshot ->
                val documents = querySnapshot.documents
                Log.d("Firestore", "getLocations $documents")
                Log.d("Firestore", "getLocations ${documents.size}")

                val response = if (documents.isNotEmpty()) {
                    documents.map { it.toObject(Locate::class.java) }
                } else {
                    arrayListOf()
                }
                callback.onSuccessListener(response)
            }
            .addOnFailureListener {
                callback.addOnFailureListener(it)
            }
    }

    fun getProfiles(callback: FirestoreResponseCallback<List<Profile?>?>) {
        userDocument.collection(COLLECTION_PROFILES).get()
            .addOnSuccessListener { querySnapshot ->
                val documents = querySnapshot.documents
                val response = if (documents.isNotEmpty()) {
                    documents.map { it.toObject(Profile::class.java) }
                } else {
                    arrayListOf()
                }
                callback.onSuccessListener(response)
            }
            .addOnFailureListener {
                callback.addOnFailureListener(it)
            }
    }

    fun haveProfiles(callback: (Boolean) -> Unit) {
        userDocument.collection(COLLECTION_PROFILES).get()
            .addOnSuccessListener { querySnapshot ->
                val documents = querySnapshot.documents
                if (documents.isNotEmpty()) {
                    callback(true)
                } else {
                    callback(false)
                }
            }
            .addOnFailureListener {
                callback(false)
            }
    }

    companion object {
        // Firestore Collection
        const val COLLECTION_DEVICES = "devices" // TODO: delete
        const val COLLECTION_USERS = "users"
        const val COLLECTION_DEPLOYMENTS = "deployments"
        const val COLLECTION_LOCATIONS = "locations"
        const val COLLECTION_PROFILES = "profiles"

        // MOCKUP
        const val USER_ID = "SPYW1VXiT68geKPdOel6"
    }
}
