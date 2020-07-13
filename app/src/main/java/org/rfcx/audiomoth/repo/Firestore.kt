package org.rfcx.audiomoth.repo

import android.content.Context
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.tasks.await
import org.rfcx.audiomoth.entity.DeploymentImage.Companion.FIELD_DEPLOYMENT_SERVER_ID
import org.rfcx.audiomoth.entity.Device
import org.rfcx.audiomoth.entity.User
import org.rfcx.audiomoth.entity.request.*
import org.rfcx.audiomoth.entity.response.DeploymentResponse
import org.rfcx.audiomoth.entity.response.GuardianDeploymentResponse
import org.rfcx.audiomoth.entity.response.LocationResponse
import org.rfcx.audiomoth.localdb.DeploymentDb
import org.rfcx.audiomoth.localdb.LocateDb
import org.rfcx.audiomoth.localdb.guardian.GuardianDeploymentDb
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
        guardianDeploymentDb: GuardianDeploymentDb,
        callback: ResponseCallback<Boolean>? = null
    ) {
        val userDocument = db.collection(COLLECTION_USERS).document(guid)
        userDocument.collection(COLLECTION_DEPLOYMENTS).get()
            .addOnSuccessListener {
                val deploymentResponses = arrayListOf<DeploymentResponse>()
                val gdResponses = arrayListOf<GuardianDeploymentResponse>()
                it.documents.forEach { doc ->
                    if (doc == null) return@forEach

                    if (doc.getString("device") == Device.GUARDIAN.value) {
                        val response = doc.toObject(GuardianDeploymentResponse::class.java)
                        response?.serverId = doc.id
                        response?.let { it1 -> gdResponses.add(it1) }
                    } else {
                        val response = doc.toObject(DeploymentResponse::class.java)
                        response?.serverId = doc.id
                        response?.let { it1 -> deploymentResponses.add(it1) }
                    }
                }

                // verify response and store deployment
                deploymentResponses.forEach { dr ->
                    deploymentDb.insertOrUpdate(dr)
                }

                // verify response and store guardian deployment
                gdResponses.forEach { dr ->
                    guardianDeploymentDb.insertOrUpdate(dr)
                }

                callback?.onSuccessCallback(true)
            }
            .addOnFailureListener {
                callback?.onFailureCallback(it.localizedMessage)
            }
    }

    fun retrieveLocations(
        locateDb: LocateDb,
        callback: ResponseCallback<List<LocationResponse>>? = null
    ) {
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
                callback?.onSuccessCallback(locationResponses)
            }
            .addOnFailureListener {
                callback?.onFailureCallback(it.localizedMessage)
            }
    }

    fun getRemotePathByServerId(serverId: String, callback: (ArrayList<String>?) -> Unit) {
        val userDocument = db.collection(COLLECTION_USERS).document(guid)
        userDocument.collection(COLLECTION_IMAGES)
            .whereEqualTo(FIELD_DEPLOYMENT_SERVER_ID, serverId).get()
            .addOnSuccessListener {
                val array = arrayListOf<String>()
                it.documents.forEach { doc ->
                    val remotePath = doc["remotePath"] as String
                    array.add(remotePath)
                }
                callback(array)
            }.addOnFailureListener {
                callback(null)
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
