package org.rfcx.companion.repo

import android.content.Context
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import java.sql.Timestamp
import java.util.*
import kotlinx.coroutines.tasks.await
import org.rfcx.companion.entity.DeploymentLocation
import org.rfcx.companion.entity.Device
import org.rfcx.companion.entity.EdgeDeployment
import org.rfcx.companion.entity.User
import org.rfcx.companion.entity.guardian.GuardianDeployment
import org.rfcx.companion.entity.request.*
import org.rfcx.companion.entity.response.*
import org.rfcx.companion.localdb.DeploymentImageDb
import org.rfcx.companion.localdb.EdgeDeploymentDb
import org.rfcx.companion.localdb.LocateDb
import org.rfcx.companion.localdb.LocationGroupDb
import org.rfcx.companion.localdb.guardian.DiagnosticDb
import org.rfcx.companion.localdb.guardian.GuardianDeploymentDb
import org.rfcx.companion.util.Preferences
import org.rfcx.companion.util.Storage
import org.rfcx.companion.util.getEmailUser

class Firestore(val context: Context) {
    val db = Firebase.firestore

    /* TODO: update get user */
    private val preferences =
        Preferences.getInstance(context)
    private val uid = preferences.getString(Preferences.USER_FIREBASE_UID, "")
    private val feedbackDocument = db.collection(COLLECTION_FEEDBACK)

    fun saveUser(user: User, uid: String, callback: (String?, Boolean) -> Unit) {
        db.collection(COLLECTION_USERS).document(uid).set(user)
            .addOnSuccessListener {
                callback(null, true)
            }
            .addOnFailureListener { e ->
                callback(e.message, false)
            }
    }

    suspend fun sendDeployment(deployment: DeploymentRequest): DocumentReference? {
        val userDocument = db.collection(COLLECTION_USERS).document(uid)
        return userDocument.collection(COLLECTION_DEPLOYMENTS).add(deployment).await()
    }

    suspend fun sendDeployment(deployment: GuardianDeploymentRequest): DocumentReference? {
        val userDocument = db.collection(COLLECTION_USERS).document(uid)
        return userDocument.collection(COLLECTION_DEPLOYMENTS).add(deployment).await()
    }

    suspend fun sendProfile(profile: GuardianProfileRequest): DocumentReference? {
        val userDocument = db.collection(COLLECTION_USERS).document(uid)
        return userDocument.collection(COLLECTION_PROFILES).add(profile).await()
    }

    suspend fun sendGroup(group: EdgeGroupRequest): DocumentReference? {
        val userDocument = db.collection(COLLECTION_USERS).document(uid)
        return userDocument.collection(COLLECTION_GROUPS).add(group).await()
    }

    suspend fun sendImage(imageRequest: ImageRequest): DocumentReference? {
        val userDocument = db.collection(COLLECTION_USERS).document(uid)
        return userDocument.collection(COLLECTION_IMAGES).add(imageRequest).await()
    }

    suspend fun sendLocation(locateRequest: LocateRequest): DocumentReference? {
        val userDocument = db.collection(COLLECTION_USERS).document(uid)
        return userDocument.collection(COLLECTION_LOCATIONS).add(locateRequest).await()
    }

    suspend fun updateLocation(locateServerId: String, locateRequest: LocateRequest) {
        val userDocument = db.collection(COLLECTION_USERS).document(uid)
        userDocument.collection(COLLECTION_LOCATIONS).document(locateServerId)
            .set(locateRequest).await()
    }

    suspend fun updateDeploymentLocation(
        serverId: String,
        deploymentLocation: DeploymentLocation,
        updatedAt: Date
    ) {
        val userDocument = db.collection(COLLECTION_USERS).document(uid)
        val updates = hashMapOf<String, Any>(
            EdgeDeployment.FIELD_LOCATION to deploymentLocation,
            EdgeDeployment.FIELD_UPDATED_AT to updatedAt
        )
        userDocument.collection(COLLECTION_DEPLOYMENTS).document(serverId)
            .update(updates).await()
    }

    suspend fun updateGuardianDeploymentLocation(
        serverId: String,
        deploymentLocation: DeploymentLocation
    ) {
        val userDocument = db.collection(COLLECTION_USERS).document(uid)
        val updates = hashMapOf<String, Any>(
            GuardianDeployment.FIELD_LOCATION to deploymentLocation
        )
        userDocument.collection(COLLECTION_DEPLOYMENTS).document(serverId)
            .update(updates).await()
    }

    suspend fun updateDeleteDeployment(serverId: String, deletedAt: Date) {
        val userDocument = db.collection(COLLECTION_USERS).document(uid)
        userDocument.collection(COLLECTION_DEPLOYMENTS).document(serverId)
            .update(EdgeDeployment.FIELD_DELETED_AT, deletedAt).await()
    }

    suspend fun sendDiagnostic(diagnosticRequest: DiagnosticRequest): DocumentReference? {
        val userDocument = db.collection(COLLECTION_USERS).document(uid)
        return userDocument.collection(COLLECTION_DIAGNOSTIC).add(diagnosticRequest).await()
    }

    suspend fun updateDiagnostic(diagnosticServerId: String, diagnosticRequest: DiagnosticRequest) {
        val userDocument = db.collection(COLLECTION_USERS).document(uid)
        userDocument.collection(COLLECTION_DIAGNOSTIC).document(diagnosticServerId)
            .set(diagnosticRequest).await()
    }

    suspend fun updateGroup(groupServerId: String, group: EdgeGroupRequest) {
        val userDocument = db.collection(COLLECTION_USERS).document(uid)
        userDocument.collection(COLLECTION_GROUPS).document(groupServerId)
            .set(group).await()
    }

    fun retrieveDeployments(
        edgeDeploymentDb: EdgeDeploymentDb,
        guardianDeploymentDb: GuardianDeploymentDb,
        callback: ResponseCallback<Boolean>? = null
    ) {
        val userDocument = db.collection(COLLECTION_USERS).document(uid)
        userDocument.collection(COLLECTION_DEPLOYMENTS).get()
            .addOnSuccessListener {
                val edgeResponses = arrayListOf<EdgeDeploymentResponse>()
                val guardianResponses = arrayListOf<GuardianDeploymentResponse>()
                // verify response
                it.documents.forEach { doc ->
                    if (doc == null) return@forEach
                    if (doc.getString("device") == Device.GUARDIAN.value) {
                        val response = doc.toObject(GuardianDeploymentResponse::class.java)
                        response?.serverId = doc.id
                        response?.let { it1 -> guardianResponses.add(it1) }
                    } else {
                        val response = doc.toObject(EdgeDeploymentResponse::class.java)
                        response?.serverId = doc.id
                        response?.let { it1 -> edgeResponses.add(it1) }
                    }
                }

                // verify response and store deployment
                edgeResponses.forEach { dr ->
                    edgeDeploymentDb.insertOrUpdate(dr)
                }

                // store guardian deployment
                guardianResponses.forEach { dr ->
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
        val userDocument = db.collection(COLLECTION_USERS).document(uid)
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

    fun retrieveLocationGroups(
        locationGroupDb: LocationGroupDb,
        callback: ResponseCallback<List<LocationGroupsResponse>>? = null
    ) {
        val userDocument = db.collection(COLLECTION_USERS).document(uid)
        userDocument.collection(COLLECTION_GROUPS).get()
            .addOnSuccessListener {
                val groupResponses = arrayListOf<LocationGroupsResponse>()
                it.documents.forEach { doc ->
                    val groupResponse = doc.toObject(LocationGroupsResponse::class.java)
                    groupResponse?.serverId = doc.id
                    groupResponse?.let { it1 -> groupResponses.add(it1) }
                }

                // verify response and store deployment
                groupResponses.forEach { lr ->
                    locationGroupDb.insertOrUpdate(lr)
                }

                callback?.onSuccessCallback(groupResponses)
            }
            .addOnFailureListener {
                callback?.onFailureCallback(it.localizedMessage)
            }
    }

    fun retrieveImages(
        edgeDeploymentDb: EdgeDeploymentDb,
        deploymentImageDb: DeploymentImageDb,
        callback: ResponseCallback<List<DeploymentImageResponse>>? = null
    ) {
        if (uid != "") {
            val userDocument = db.collection(COLLECTION_USERS).document(uid)
            userDocument.collection(COLLECTION_IMAGES).get()
                .addOnSuccessListener {
                    val deploymentImageResponses = arrayListOf<DeploymentImageResponse>()
                    it.documents.forEach { doc ->
                        val deploymentImageResponse =
                            doc.toObject(DeploymentImageResponse::class.java)
                        deploymentImageResponse?.let { it1 -> deploymentImageResponses.add(it1) }
                    }

                    deploymentImageResponses.forEach { lr ->
                        val edgeDeploymentId =
                            edgeDeploymentDb.getDeploymentByServerId(lr.deploymentServerId)
                        deploymentImageDb.insertOrUpdate(lr, edgeDeploymentId?.id)
                    }

                    callback?.onSuccessCallback(deploymentImageResponses)
                }
                .addOnFailureListener {
                    callback?.onFailureCallback(it.localizedMessage)
                }
        }
    }

    fun retrieveDiagnostics(
        diagnosticDb: DiagnosticDb,
        callback: ResponseCallback<List<DiagnosticResponse>>? = null
    ) {
        val userDocument = db.collection(COLLECTION_USERS).document(uid)
        userDocument.collection(COLLECTION_DIAGNOSTIC).get()
            .addOnSuccessListener {
                val diagnosticResponses = arrayListOf<DiagnosticResponse>()
                it.documents.forEach { doc ->
                    val diagnosticResponse = doc.toObject(DiagnosticResponse::class.java)
                    diagnosticResponse?.serverId = doc.id
                    diagnosticResponse?.let { it1 -> diagnosticResponses.add(it1) }
                }
                // verify response and store diagnostic
                diagnosticResponses.forEach { diagnostic ->
                    diagnosticDb.insertOrUpdate(diagnostic)
                }
                callback?.onSuccessCallback(diagnosticResponses)
            }
            .addOnFailureListener {
                callback?.onFailureCallback(it.localizedMessage)
            }
    }

    fun saveFeedback(
        text: String,
        uris: List<String>?,
        callback: (Boolean) -> Unit
    ) {
        val docData = hashMapOf(
            "userId" to uid,
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
        const val COLLECTION_DIAGNOSTIC = "diagnostics"
        const val COLLECTION_GROUPS = "groups"
    }
}
