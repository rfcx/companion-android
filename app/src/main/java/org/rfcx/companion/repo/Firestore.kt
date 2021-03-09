package org.rfcx.companion.repo

import android.content.Context
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.tasks.await
import org.rfcx.companion.entity.User
import org.rfcx.companion.entity.request.EdgeGroupRequest
import org.rfcx.companion.entity.response.DiagnosticResponse
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

    suspend fun sendGroup(group: EdgeGroupRequest, docId: String) {
        val userDocument = db.collection(COLLECTION_USERS).document(uid)
        userDocument.collection(COLLECTION_GROUPS).document(docId).set(group).await()
    }

    suspend fun updateGroup(groupServerId: String, group: EdgeGroupRequest) {
        val userDocument = db.collection(COLLECTION_USERS).document(uid)
        userDocument.collection(COLLECTION_GROUPS).document(groupServerId)
            .set(group).await()
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
