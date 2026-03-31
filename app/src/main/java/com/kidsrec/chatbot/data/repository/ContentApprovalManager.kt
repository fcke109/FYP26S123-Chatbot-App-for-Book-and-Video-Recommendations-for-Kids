package com.kidsrec.chatbot.data.repository

import android.util.Log
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.kidsrec.chatbot.data.model.ApprovalStatus
import com.kidsrec.chatbot.data.model.ContentApproval
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ContentApprovalManager @Inject constructor(
    private val firestore: FirebaseFirestore
) {
    private val collection = firestore.collection("contentApprovals")

    fun getPendingApprovalsFlow(parentId: String): Flow<List<ContentApproval>> = callbackFlow {
        val listener = collection
            .whereEqualTo("parentId", parentId)
            .whereEqualTo("status", ApprovalStatus.PENDING.name)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("ContentApproval", "Error loading approvals", error)
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val approvals = snapshot?.toObjects(ContentApproval::class.java) ?: emptyList()
                trySend(approvals)
            }
        awaitClose { listener.remove() }
    }

    fun getApprovalStatusFlow(childId: String, contentId: String): Flow<ApprovalStatus?> = callbackFlow {
        val listener = collection
            .whereEqualTo("childId", childId)
            .whereEqualTo("contentId", contentId)
            .limit(1)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(null)
                    return@addSnapshotListener
                }
                val approval = snapshot?.toObjects(ContentApproval::class.java)?.firstOrNull()
                trySend(approval?.status)
            }
        awaitClose { listener.remove() }
    }

    suspend fun requestApproval(
        childId: String,
        parentId: String,
        contentId: String,
        contentTitle: String,
        contentType: String,
        contentUrl: String,
        contentImageUrl: String
    ): Result<Unit> {
        return try {
            val docRef = collection.document()
            val approval = ContentApproval(
                id = docRef.id,
                childId = childId,
                parentId = parentId,
                contentId = contentId,
                contentTitle = contentTitle,
                contentType = contentType,
                contentUrl = contentUrl,
                contentImageUrl = contentImageUrl,
                status = ApprovalStatus.PENDING,
                requestedAt = Timestamp.now()
            )
            docRef.set(approval).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("ContentApproval", "Failed to request approval", e)
            Result.failure(e)
        }
    }

    suspend fun approveContent(approvalId: String): Result<Unit> {
        return try {
            collection.document(approvalId).update(
                mapOf(
                    "status" to ApprovalStatus.APPROVED.name,
                    "decidedAt" to Timestamp.now()
                )
            ).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun rejectContent(approvalId: String): Result<Unit> {
        return try {
            collection.document(approvalId).update(
                mapOf(
                    "status" to ApprovalStatus.REJECTED.name,
                    "decidedAt" to Timestamp.now()
                )
            ).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun isContentApproved(childId: String, contentId: String): Boolean {
        return try {
            val snapshot = collection
                .whereEqualTo("childId", childId)
                .whereEqualTo("contentId", contentId)
                .whereEqualTo("status", ApprovalStatus.APPROVED.name)
                .limit(1)
                .get()
                .await()
            !snapshot.isEmpty
        } catch (e: Exception) {
            false
        }
    }
}
