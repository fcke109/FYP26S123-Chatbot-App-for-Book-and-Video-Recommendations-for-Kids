package com.kidsrec.chatbot.data.repository

import android.util.Log
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.kidsrec.chatbot.data.model.AccountType
import com.kidsrec.chatbot.data.model.Favorite
import com.kidsrec.chatbot.data.model.InviteCode
import com.kidsrec.chatbot.data.model.LoginAttempt
import com.kidsrec.chatbot.data.model.ReadingHistory
import com.kidsrec.chatbot.data.model.User
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.Calendar
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * AccountManager: Handles all user login, registration, and profile data.
 */
@Singleton
class AccountManager @Inject constructor(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) {
    val currentUser: FirebaseUser?
        get() = auth.currentUser

    fun getCurrentUserId(): String? = auth.currentUser?.uid

    suspend fun signIn(email: String, password: String): Result<FirebaseUser> {
        return try {
            val result = auth.signInWithEmailAndPassword(email, password).await()
            val user = result.user ?: return Result.failure(Exception("Sign in failed"))

            // Track successful login attempt
            trackLoginAttempt(email, user.uid, true, "")

            Result.success(user)
        } catch (e: Exception) {
            // Track failed login attempt
            trackLoginAttempt(email, "", false, e.message ?: "Unknown error")
            Result.failure(e)
        }
    }

    suspend fun signUp(
        email: String,
        password: String,
        name: String,
        age: Int,
        interests: List<String>,
        readingLevel: String
    ): Result<FirebaseUser> {
        var createdUser: FirebaseUser? = null
        return try {
            val result = auth.createUserWithEmailAndPassword(email, password).await()
            val user = result.user ?: return Result.failure(Exception("User creation failed"))
            createdUser = user

            val userDoc = User(
                id = user.uid,
                name = name,
                email = email,
                age = age,
                accountType = AccountType.CHILD,
                interests = interests,
                readingLevel = readingLevel
            )

            firestore.collection("users")
                .document(user.uid)
                .set(userDoc)
                .await()

            Result.success(user)
        } catch (e: Exception) {
            // If auth account was created but Firestore write failed, clean up
            createdUser?.delete()
            auth.signOut()
            Result.failure(e)
        }
    }

    // ── Parent signup ──────────────────────────────────────────────
    suspend fun signUpParent(
        email: String,
        password: String,
        name: String
    ): Result<FirebaseUser> {
        var createdUser: FirebaseUser? = null
        return try {
            val result = auth.createUserWithEmailAndPassword(email, password).await()
            val user = result.user ?: return Result.failure(Exception("User creation failed"))
            createdUser = user

            // Send email verification for parent accounts
            user.sendEmailVerification().await()

            val userDoc = User(
                id = user.uid,
                name = name,
                email = email,
                age = 0,
                accountType = AccountType.PARENT,
                interests = emptyList(),
                readingLevel = "Beginner"
            )

            firestore.collection("users")
                .document(user.uid)
                .set(userDoc)
                .await()

            Result.success(user)
        } catch (e: Exception) {
            // If auth account was created but Firestore write failed, clean up
            createdUser?.delete()
            auth.signOut()
            Result.failure(e)
        }
    }

    // ── Check email verification status ───────────────────────────
    suspend fun isEmailVerified(): Boolean {
        auth.currentUser?.reload()?.await()
        return auth.currentUser?.isEmailVerified ?: false
    }

    suspend fun resendVerificationEmail(): Result<Unit> {
        return try {
            auth.currentUser?.sendEmailVerification()?.await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ── Child signup with invite code ──────────────────────────────
    suspend fun signUpChild(
        email: String,
        password: String,
        name: String,
        age: Int,
        interests: List<String>,
        readingLevel: String,
        inviteCode: String
    ): Result<FirebaseUser> {
        var createdUser: FirebaseUser? = null
        return try {
            // 1. Create Firebase Auth account FIRST (so we're authenticated for Firestore)
            val result = auth.createUserWithEmailAndPassword(email, password).await()
            val user = result.user ?: return Result.failure(Exception("User creation failed"))
            createdUser = user

            // Force token refresh so Firestore recognizes the new user
            user.getIdToken(true).await()

            // 2. Now validate the invite code (requires authentication)
            val codeResult = validateInviteCode(inviteCode)
            if (codeResult.isFailure) {
                // Invalid code — clean up the auth account we just created
                user.delete().await()
                auth.signOut()
                return Result.failure(codeResult.exceptionOrNull()
                    ?: Exception("Invalid invite code"))
            }
            val code = codeResult.getOrThrow()

            // 3. Create child doc, mark invite used, link to parent — atomically
            val childDoc = User(
                id = user.uid,
                name = name,
                email = email,
                age = age,
                accountType = AccountType.CHILD,
                parentId = code.parentId,
                interests = interests,
                readingLevel = readingLevel
            )
            val batch = firestore.batch()
            batch.set(
                firestore.collection("users").document(user.uid),
                childDoc
            )
            batch.update(
                firestore.collection("inviteCodes").document(inviteCode.uppercase()),
                "used", true
            )
            batch.update(
                firestore.collection("users").document(code.parentId),
                "childIds", FieldValue.arrayUnion(user.uid)
            )
            batch.commit().await()

            Result.success(user)
        } catch (e: Exception) {
            Log.e("AccountManager", "Child signup failed: ${e.message}", e)
            createdUser?.delete()
            auth.signOut()
            Result.failure(e)
        }
    }

    // ── Invite code generation ────────────────────────────────────
    suspend fun generateInviteCode(parentId: String, parentName: String): Result<String> {
        return try {
            var code: String
            var attempts = 0

            // Generate unique 6-char code
            do {
                code = UUID.randomUUID().toString()
                    .replace("-", "")
                    .take(6)
                    .uppercase()
                val existing = firestore.collection("inviteCodes")
                    .document(code)
                    .get()
                    .await()
                attempts++
            } while (existing.exists() && attempts < 5)

            if (attempts >= 5) {
                return Result.failure(Exception("Could not generate unique code. Try again."))
            }

            // Expire in 24 hours
            val now = Timestamp.now()
            val calendar = Calendar.getInstance().apply {
                time = now.toDate()
                add(Calendar.HOUR_OF_DAY, 24)
            }
            val expiresAt = Timestamp(calendar.time)

            val inviteCode = InviteCode(
                code = code,
                parentId = parentId,
                parentName = parentName,
                createdAt = now,
                expiresAt = expiresAt,
                used = false
            )

            firestore.collection("inviteCodes")
                .document(code)
                .set(inviteCode)
                .await()

            Result.success(code)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ── Invite code validation ────────────────────────────────────
    suspend fun validateInviteCode(code: String): Result<InviteCode> {
        return try {
            val doc = firestore.collection("inviteCodes")
                .document(code.uppercase())
                .get()
                .await()

            if (!doc.exists()) {
                return Result.failure(Exception("Invite code not found. Please check and try again."))
            }

            val inviteCode = doc.toObject(InviteCode::class.java)
                ?: return Result.failure(Exception("Invalid invite code data."))

            if (inviteCode.used) {
                return Result.failure(Exception("This invite code has already been used."))
            }

            if (inviteCode.expiresAt.toDate().before(Timestamp.now().toDate())) {
                return Result.failure(Exception("This invite code has expired. Ask your parent for a new one."))
            }

            Result.success(inviteCode)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ── Get children for a parent ─────────────────────────────────
    fun getChildrenFlow(parentId: String): Flow<List<User>> = callbackFlow {
        val listener = firestore.collection("users")
            .whereEqualTo("parentId", parentId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("AccountManager", "Error fetching children: ${error.message}")
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val children = snapshot?.toObjects(User::class.java) ?: emptyList()
                trySend(children)
            }
        awaitClose { listener.remove() }
    }

    // ── Get child's favorites (for parent dashboard) ──────────────
    fun getChildFavoritesFlow(childId: String): Flow<List<Favorite>> = callbackFlow {
        val listener = firestore.collection("favorites")
            .document(childId)
            .collection("items")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("AccountManager", "Error fetching child favorites: ${error.message}")
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val favorites = snapshot?.toObjects(Favorite::class.java) ?: emptyList()
                trySend(favorites)
            }
        awaitClose { listener.remove() }
    }

    // ── Get child's reading history (for parent dashboard) ────────
    fun getChildReadingHistoryFlow(childId: String): Flow<List<ReadingHistory>> = callbackFlow {
        val listener = firestore.collection("readingHistory")
            .document(childId)
            .collection("sessions")
            .orderBy("openedAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .limit(20)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("AccountManager", "Error fetching child history: ${error.message}")
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val history = snapshot?.toObjects(ReadingHistory::class.java) ?: emptyList()
                trySend(history)
            }
        awaitClose { listener.remove() }
    }

    // ── Update a child's content filters (for parent) ─────────────
    suspend fun updateChildFilters(
        childId: String,
        maxAgeRating: Int,
        allowVideos: Boolean
    ): Result<Unit> {
        return try {
            firestore.collection("users")
                .document(childId)
                .update(
                    mapOf(
                        "contentFilters.maxAgeRating" to maxAgeRating,
                        "contentFilters.allowVideos" to allowVideos
                    )
                )
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun signInAnonymously(): Result<FirebaseUser> {
        return try {
            val result = auth.signInAnonymously().await()
            result.user?.let { Result.success(it) }
                ?: Result.failure(Exception("Anonymous sign in failed"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun signOut() {
        auth.signOut()
    }

    suspend fun getUser(userId: String): User? {
        return try {
            val snapshot = firestore.collection("users").document(userId).get().await()
            snapshot.toObject(User::class.java)
        } catch (e: Exception) {
            null
        }
    }

    fun getUserFlow(userId: String): Flow<User?> = callbackFlow {
        val listener = firestore.collection("users")
            .document(userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("AccountManager", "Error fetching user flow: ${error.message}")
                    trySend(null)
                    return@addSnapshotListener
                }
                val user = snapshot?.toObject(User::class.java)
                trySend(user)
            }

        awaitClose { listener.remove() }
    }

    fun getAllUsersFlow(): Flow<List<User>> = callbackFlow {
        val listener = firestore.collection("users")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("AccountManager", "Error fetching all users flow: ${error.message}")
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val users = snapshot?.toObjects(User::class.java) ?: emptyList()
                trySend(users)
            }
        awaitClose { listener.remove() }
    }

    suspend fun updateUser(user: User): Result<Unit> {
        return try {
            firestore.collection("users")
                .document(user.id)
                .set(user)
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateFcmToken(userId: String, token: String): Result<Unit> {
        return try {
            firestore.collection("users")
                .document(userId)
                .update("fcmToken", token)
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun trackLoginAttempt(email: String, userId: String, success: Boolean, failureReason: String) {
        try {
            val loginAttempt = LoginAttempt(
                userId = userId,
                email = email,
                success = success,
                failureReason = if (success) "" else failureReason
            )
            firestore.collection("loginAttempts")
                .add(loginAttempt)
                .await()
        } catch (e: Exception) {
            // Log error but don't fail the sign in process
            android.util.Log.w("AccountManager", "Failed to track login attempt", e)
        }
    }
}
