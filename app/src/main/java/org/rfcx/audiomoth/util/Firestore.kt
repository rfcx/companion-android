package org.rfcx.audiomoth.util

import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import org.rfcx.audiomoth.entity.Deployment
import org.rfcx.audiomoth.entity.Locate
import org.rfcx.audiomoth.entity.Profile
import org.rfcx.audiomoth.entity.User

interface FirestoreResponseCallback<T> {
    fun onSuccessListener(response: T)
    fun addOnFailureListener(exception: Exception)
}

class Firestore {
    val db = Firebase.firestore
    /* TODO: update get user */
    private val userDocument = db.collection(COLLECTION_USERS).document(USER_ID)

    fun saveUser(guid: String, user: User, callback: (String?, Boolean) -> Unit) {
        db.collection(COLLECTION_USERS).document(guid)
            .set(user)
            .addOnSuccessListener {
                callback(null, true)
            }
            .addOnFailureListener { e ->
                callback(e.message, false)
            }
    }

    fun saveLocation(guid: String, locate: Locate, callback: (String?, Boolean) -> Unit) {
        db.collection(COLLECTION_USERS).document(guid).collection(COLLECTION_LOCATIONS)
            .add(locate)
            .addOnSuccessListener { documentReference ->
                callback(documentReference.id, true)
            }
            .addOnFailureListener { e ->
                callback(e.message, false)
            }
    }

    fun saveProfile(guid: String, profile: Profile, callback: (String?, Boolean) -> Unit) {
        db.collection(COLLECTION_USERS).document(guid).collection(COLLECTION_PROFILES)
            .add(profile)
            .addOnSuccessListener { documentReference ->
                callback(documentReference.id, true)
            }
            .addOnFailureListener { e ->
                callback(e.message, false)
            }
    }

    fun getDeployments(callback: FirestoreResponseCallback<List<Deployment>>) {
        userDocument.collection(COLLECTION_DEPLOYMENTS).get()
            .addOnSuccessListener { querySnapshot ->
                val documents = querySnapshot.documents
                val response = if (documents.isNotEmpty()) {
                    val deploymentList = arrayListOf<Deployment>()
                    documents.forEach {
                        val obj = it.toObject(Deployment::class.java)
                        obj?.let { it1 -> deploymentList.add(it1) }
                    }
                    deploymentList
                } else {
                    listOf<Deployment>()
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
        const val TAG = "Firestore"

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
