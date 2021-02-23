package org.rfcx.companion.repo

import android.content.Context
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.tasks.await
import org.rfcx.companion.entity.Device
import org.rfcx.companion.entity.User
import org.rfcx.companion.entity.request.*
import org.rfcx.companion.entity.response.*
import org.rfcx.companion.localdb.DeploymentImageDb
import org.rfcx.companion.localdb.EdgeDeploymentDb
import org.rfcx.companion.localdb.guardian.DiagnosticDb
import org.rfcx.companion.localdb.guardian.GuardianDeploymentDb
import org.rfcx.companion.util.Preferences
import org.rfcx.companion.util.Storage
import org.rfcx.companion.util.getEmailUser
import org.rfcx.companion.util.getUserNickname
import java.util.*

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

    suspend fun sendDeployment(deployment: GuardianDeploymentRequest): DocumentReference? {
        val userDocument = db.collection(COLLECTION_USERS).document(uid)
        return userDocument.collection(COLLECTION_DEPLOYMENTS).add(deployment).await()
    }

    suspend fun sendProfile(profile: GuardianProfileRequest): DocumentReference? {
        val userDocument = db.collection(COLLECTION_USERS).document(uid)
        return userDocument.collection(COLLECTION_PROFILES).add(profile).await()
    }

    suspend fun sendGroup(group: EdgeGroupRequest, docId: String) {
        val userDocument = db.collection(COLLECTION_USERS).document(uid)
        userDocument.collection(COLLECTION_GROUPS).document(docId).set(group).await()
    }

    suspend fun sendImage(imageRequest: ImageRequest): DocumentReference? {
        val userDocument = db.collection(COLLECTION_USERS).document(uid)
        return userDocument.collection(COLLECTION_IMAGES).add(imageRequest).await()
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

    fun retrieveImages(
        edgeDeploymentDb: EdgeDeploymentDb,
        guardianDeploymentDb: GuardianDeploymentDb,
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
                        val guardianDeploymentId =
                            guardianDeploymentDb.getDeploymentByServerId(lr.deploymentServerId)

                        if (edgeDeploymentId != null) {
                            deploymentImageDb.insertOrUpdate(lr, edgeDeploymentId.id, Device.AUDIOMOTH.value)
                        }

                        if (guardianDeploymentId != null) {
                            deploymentImageDb.insertOrUpdate(lr, guardianDeploymentId.id, Device.GUARDIAN.value)
                        }
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
            "timeStamp" to Date(),
            "name" to context.getUserNickname()
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
