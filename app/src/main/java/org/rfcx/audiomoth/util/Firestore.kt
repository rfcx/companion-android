package org.rfcx.audiomoth.util

import android.content.Context
import android.view.View
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import org.rfcx.audiomoth.entity.*
import org.rfcx.audiomoth.entity.Deployment.Companion.DEPLOYED_AT
import org.rfcx.audiomoth.entity.Deployment.Companion.IS_LATEST
import org.rfcx.audiomoth.entity.Deployment.Companion.LAST_DEPLOYMENT
import org.rfcx.audiomoth.entity.Deployment.Companion.PHOTOS
import java.sql.Timestamp

interface FirestoreResponseCallback<T> {
    fun onSuccessListener(response: T)
    fun addOnFailureListener(exception: Exception)
}

class Firestore(val context: Context) {
    val db = Firebase.firestore
    /* TODO: update get user */
    private val preferences = Preferences.getInstance(context)
    private val guid = preferences.getString(Preferences.USER_GUID, "")
    private val userDocument = db.collection(COLLECTION_USERS).document(guid)
    private val feedbackDocument = db.collection(COLLECTION_FEEDBACK)

    fun saveUser(user: User, callback: (String?, Boolean) -> Unit) {
        userDocument.set(user)
            .addOnSuccessListener {
                callback(null, true)
            }
            .addOnFailureListener { e ->
                callback(e.message, false)
            }
    }

//    fun saveLocation(locate: Locate, callback: (String?, Boolean) -> Unit) {
//        userDocument.collection(COLLECTION_LOCATIONS)
//            .add(locate)
//            .addOnSuccessListener { documentReference ->
//                callback(documentReference.id, true)
//            }
//            .addOnFailureListener { e ->
//                callback(e.message, false)
//            }
//    }

    fun saveProfile(profile: Profile, callback: (String?, Boolean) -> Unit) {
        userDocument.collection(COLLECTION_PROFILES)
            .add(profile)
            .addOnSuccessListener { documentReference ->
                callback(documentReference.id, true)
            }
            .addOnFailureListener { e ->
                callback(e.message, false)
            }
    }

    fun saveDeployment(deployment: Deployment, callback: (String?, Boolean) -> Unit) {
        userDocument.collection(COLLECTION_DEPLOYMENTS)
            .add(deployment)
            .addOnSuccessListener { documentReference ->
                callback(documentReference.id, true)
            }
            .addOnFailureListener { e ->
                callback(e.message, false)
            }
    }

//    fun getDeployments(callback: FirestoreResponseCallback<List<Deployment>>) {
//        userDocument.collection(COLLECTION_DEPLOYMENTS)
//            .orderBy(DEPLOYED_AT, Query.Direction.ASCENDING).get()
//            .addOnSuccessListener { querySnapshot ->
//                val documents = querySnapshot.documents
//                val response = if (documents.isNotEmpty()) {
//                    val deploymentList = arrayListOf<Deployment>()
//                    documents.forEach {
//                        val obj = it.toObject(Deployment::class.java)
//                        obj?.let { it1 ->
//                            if (it1.latest) {
//                                deploymentList.add(it1)
//                            }
//                        }
//                    }
//                    deploymentList
//                } else {
//                    listOf<Deployment>()
//                }
//                callback.onSuccessListener(response)
//            }
//            .addOnFailureListener {
//                callback.addOnFailureListener(it)
//            }
//
//    }
//
//    fun getLocations(callback: FirestoreResponseCallback<List<LocateItem?>>) {
//        userDocument.collection(COLLECTION_LOCATIONS).get()
//            .addOnSuccessListener { querySnapshot ->
//                val documents = querySnapshot.documents
//                val response = ArrayList<LocateItem>()
//
//                if (documents.isNotEmpty()) {
//                    documents.map {
//                        val locate = it.toObject(Locate::class.java)
//                        response.add(LocateItem(locate, it.id))
//                    }
//                } else {
//                    arrayListOf()
//                }
//                callback.onSuccessListener(response)
//            }
//            .addOnFailureListener {
//                callback.addOnFailureListener(it)
//            }
//    }

    fun updateLocation(locateId: String, deploymentId: String) {
        userDocument.collection(COLLECTION_LOCATIONS)
            .document(locateId)
            .update(LAST_DEPLOYMENT, deploymentId)
    }

    fun updateDeployment(deploymentId: String, photos: ArrayList<String>) {
        userDocument.collection(COLLECTION_DEPLOYMENTS)
            .document(deploymentId)
            .update(PHOTOS, photos)
    }

//    fun updateIsLatest(locateId: String, callback: (Boolean) -> Unit) {
//        userDocument.collection(COLLECTION_DEPLOYMENTS).get()
//            .addOnSuccessListener { querySnapshot ->
//                val documents = querySnapshot.documents
//                if (documents.size == 0) callback(true)
//                documents.map {
//                    val deployment = it.toObject(Deployment::class.java)
//                    if (deployment?.locationLocation?.id == locateId) {
//                        userDocument.collection(COLLECTION_DEPLOYMENTS)
//                            .document(it.id)
//                            .update(IS_LATEST, false)
//                    }
//                    if (it.id == documents.last().id) {
//                        callback(true)
//                    }
//                }
//            }.addOnFailureListener {
//                callback(false)
//            }
//    }

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

    fun haveLocations(callback: (Boolean) -> Unit) {
        userDocument.collection(COLLECTION_LOCATIONS).get()
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

    fun saveFeedback(
        text: String, uris: List<String>?, callback: (Boolean) -> Unit
    ) {
        val docData = hashMapOf(
            "userId" to guid,
            "from" to context.getEmailUser(),
            "inputFeedback" to text,
            "pathImages" to arrayListOf<String>(),
            "timeStamp" to Timestamp(System.currentTimeMillis()).toString()
        )

        feedbackDocument.add(docData)
            .addOnSuccessListener {
                callback(true)
                if (uris != null) {
                    Storage(context).uploadImagesOfFeedback(uris) { success, pathImages ->
                        if (success) {
                            val docData = hashMapOf("pathImages" to pathImages)
                            it.update(docData as Map<String, Any>)
                        }
                    }
                }
            }.addOnFailureListener { callback(false) }
    }

    companion object {
        const val TAG = "Firestore"

        // Firestore Collection
        const val COLLECTION_USERS = "users"
        const val COLLECTION_FEEDBACK = "feedback"
        const val COLLECTION_DEPLOYMENTS = "deployments"
        const val COLLECTION_LOCATIONS = "locations"
        const val COLLECTION_PROFILES = "profiles"
    }
}
