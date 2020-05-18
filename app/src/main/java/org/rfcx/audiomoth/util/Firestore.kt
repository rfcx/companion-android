package org.rfcx.audiomoth.util

import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import org.rfcx.audiomoth.entity.Deployment
import org.rfcx.audiomoth.entity.Location
import org.rfcx.audiomoth.entity.Profile
import org.rfcx.audiomoth.entity.User
import org.rfcx.audiomoth.entity.User.Companion.FIELD_NAME


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

    fun saveDevice(user: User, callback: FirestoreCallback) {
        db.collection(COLLECTION_DEVICES).document().set(user)
            .addOnCompleteListener {
                callback.onCompleteListener()
            }.addOnFailureListener {
                callback.addOnFailureListener(it)
            }
    }

    fun getDocumentIdOfUser(userName: String, callback: FirestoreResponseCallback<String?>) {
        db.collection(COLLECTION_USERS).whereEqualTo(FIELD_NAME, userName).limit(1).get()
            .addOnSuccessListener { querySnapshot ->
                val documents = querySnapshot.documents
                if (documents.isNotEmpty()) {
                    val users = documents.map { it.id }
                    callback.onSuccessListener(users[0])
                } else {
                    callback.onSuccessListener(null)
                }
            }
            .addOnFailureListener {
                callback.addOnFailureListener(it)
            }
    }

    fun getDeployments(
        documentId: String,
        callback: FirestoreResponseCallback<List<Deployment?>?>
    ) {
        db.collection(COLLECTION_USERS).document(documentId).collection(COLLECTION_DEPLOYMENTS)
            .get()
            .addOnSuccessListener { querySnapshot ->
                val documents = querySnapshot.documents
                if (documents.isNotEmpty()) {
                    val deployments = documents.map { it.toObject(Deployment::class.java) }
                    callback.onSuccessListener(deployments)
                } else {
                    callback.onSuccessListener(null)
                }
            }
            .addOnFailureListener {
                callback.addOnFailureListener(it)
            }
    }

    fun getLocations(
        documentId: String,
        callback: FirestoreResponseCallback<List<Location?>?>
    ) {
        db.collection(COLLECTION_USERS).document(documentId).collection(COLLECTION_LOCATIONS)
            .get()
            .addOnSuccessListener { querySnapshot ->
                val documents = querySnapshot.documents
                if (documents.isNotEmpty()) {
                    val locations = documents.map { it.toObject(Location::class.java) }
                    callback.onSuccessListener(locations)
                } else {
                    callback.onSuccessListener(null)
                }
            }
            .addOnFailureListener {
                callback.addOnFailureListener(it)
            }
    }

    fun getProfiles(
        documentId: String,
        callback: FirestoreResponseCallback<List<Profile?>?>
    ) {
        db.collection(COLLECTION_USERS).document(documentId).collection(COLLECTION_PROFILES)
            .get()
            .addOnSuccessListener { querySnapshot ->
                val documents = querySnapshot.documents
                if (documents.isNotEmpty()) {
                    val profiles = documents.map { it.toObject(Profile::class.java) }
                    callback.onSuccessListener(profiles)
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
        const val COLLECTION_DEVICES = "devices" // TODO: delete
        const val COLLECTION_USERS = "users"
        const val COLLECTION_DEPLOYMENTS = "deployments"
        const val COLLECTION_LOCATIONS = "locations"
        const val COLLECTION_PROFILES = "profiles"
    }
}
