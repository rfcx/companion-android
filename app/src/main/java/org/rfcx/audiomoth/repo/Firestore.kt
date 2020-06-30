package org.rfcx.audiomoth.repo

import android.content.Context
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.tasks.await
import org.rfcx.audiomoth.entity.User
import org.rfcx.audiomoth.entity.request.*
import org.rfcx.audiomoth.entity.response.DeploymentResponse
import org.rfcx.audiomoth.entity.response.LocationResponse
import org.rfcx.audiomoth.localdb.DeploymentDb
import org.rfcx.audiomoth.localdb.LocateDb
import org.rfcx.audiomoth.util.Preferences
import org.rfcx.audiomoth.util.Storage
import org.rfcx.audiomoth.util.getEmailUser
import java.sql.Timestamp

class Firestore(val context: Context) {
    val db = Firebase.firestore

    /* TODO: update get user */
    private val preferences =
        Preferences.getInstance(context)
    private val guid = preferences.getString(Preferences.USER_GUID, "")
    private val feedbackDocument = db.collection(COLLECTION_FEEDBACK)

    fun saveUser(user: User, guid: String, callback: (String?, Boolean) -> Unit) {
        db.collection(COLLECTION_USERS).document(guid).set(user)
            .addOnSuccessListener {
                callback(null, true)
            }
            .addOnFailureListener { e ->
                callback(e.message, false)
            }
    }

    suspend fun sendDeployment(deployment: DeploymentRequest): DocumentReference? {
        val userDocument = db.collection(COLLECTION_USERS).document(guid)
        return userDocument.collection(COLLECTION_DEPLOYMENTS).add(deployment).await()
    }

    suspend fun sendDeployment(deployment: GuardianDeploymentRequest): DocumentReference? {
        val userDocument = db.collection(COLLECTION_USERS).document(guid)
        return userDocument.collection(COLLECTION_DEPLOYMENTS).add(deployment).await()
    }

    suspend fun sendProfile(profile: ProfileRequest): DocumentReference? {
        val userDocument = db.collection(COLLECTION_USERS).document(guid)
        return userDocument.collection(COLLECTION_PROFILES).add(profile).await()
    }

    suspend fun sendProfile(profile: GuardianProfileRequest): DocumentReference? {
        val userDocument = db.collection(COLLECTION_USERS).document(guid)
        return userDocument.collection(COLLECTION_PROFILES).add(profile).await()
    }

    suspend fun sendImage(imageRequest: ImageRequest): DocumentReference? {
        val userDocument = db.collection(COLLECTION_USERS).document(guid)
        return userDocument.collection(COLLECTION_IMAGES).add(imageRequest).await()
    }

    suspend fun sendLocation(locateRequest: LocateRequest): DocumentReference? {
        val userDocument = db.collection(COLLECTION_USERS).document(guid)
        return userDocument.collection(COLLECTION_LOCATIONS).add(locateRequest).await()
    }

    suspend fun updateLocation(locateServerId: String, locateRequest: LocateRequest) {
        val userDocument = db.collection(COLLECTION_USERS).document(guid)
        userDocument.collection(COLLECTION_LOCATIONS).document(locateServerId)
            .set(locateRequest).await()
    }

    fun retrieveDeployments(
        deploymentDb: DeploymentDb,
        callback: ResponseCallback<List<DeploymentResponse>>
    ) {
        val userDocument = db.collection(COLLECTION_USERS).document(guid)
        userDocument.collection(COLLECTION_DEPLOYMENTS).get()
            .addOnSuccessListener {
                val deploymentResponses = arrayListOf<DeploymentResponse>()
                it.documents.forEach { doc ->
                    if (doc != null) {
                        val deploymentResponse = doc.toObject(DeploymentResponse::class.java)
                        deploymentResponse?.serverId = doc.id
                        deploymentResponse?.let { it1 -> deploymentResponses.add(it1) }
                    }
                }

                // verify response and store deployment
                deploymentResponses.forEach { dr ->
                    deploymentDb.insertOrUpdate(dr)
                }
                callback.onSuccessCallback(deploymentResponses)
            }
            .addOnFailureListener {
                callback.onFailureCallback(it.localizedMessage)
            }
    }

    fun retrieveLocations(locateDb: LocateDb, callback: ResponseCallback<List<LocationResponse>>) {
        val userDocument = db.collection(COLLECTION_USERS).document(guid)
        userDocument.collection(COLLECTION_LOCATIONS).get()
            .addOnSuccessListener {
                val locationResponses = arrayListOf<LocationResponse>()
                it.documents.forEach { doc ->
                    val locationResponse = doc.toObject(LocationResponse::class.java)
                    locationResponse?.serverId = doc.id
                    locationResponse?.let { it1 -> locationResponses.add(it1) }
                }

                // verify response and store deployment
                locationResponses.forEach { lr ->
                    locateDb.insertOrUpdate(lr)
                }
                callback.onSuccessCallback(locationResponses)
            }
            .addOnFailureListener {
                callback.onFailureCallback(it.localizedMessage)
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
                    Storage(context)
                        .uploadImagesOfFeedback(uris) { success, pathImages ->
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
        const val COLLECTION_IMAGES = "images"
    }
}
