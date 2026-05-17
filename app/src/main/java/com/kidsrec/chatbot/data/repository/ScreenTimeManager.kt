package com.kidsrec.chatbot.data.repository

import android.util.Log
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.kidsrec.chatbot.data.model.ScreenTimeSession
import com.kidsrec.chatbot.data.model.SessionEntry
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

// Handles screen time tracking for users
@Singleton
class ScreenTimeManager @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val accountManager: AccountManager
) {
    // Stores current active session start time
    private var sessionStartTime: Timestamp? = null


    // Gets today's date in yyyy-MM-dd format
    private fun todayDate(): String {
        return SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
    }

    // Firestore reference for daily screen time data
    private fun sessionRef(userId: String, date: String) =
        firestore.collection("screenTime")
            .document(userId)
            .collection("daily")
            .document(date)

    // Starts a new screen time session
    fun startSession(userId: String) {
        // Prevent starting multiple sessions at once
        if (sessionStartTime != null) return
        sessionStartTime = Timestamp.now()
    }

    // Ends current session and saves duration
    suspend fun endSession(userId: String) {
        // Check if session exists
        val start = sessionStartTime ?: run {
            Log.w("ScreenTimeManager", "endSession called without active session for $userId")
            return
        }
        sessionStartTime = null

        // Calculate session duration
        val now = Timestamp.now()
        val durationMinutes = ((now.seconds - start.seconds) / 60).toInt().coerceAtLeast(1)
        val date = todayDate()
        val ref = sessionRef(userId, date)

        try {
            // Check if today's record already exists
            val doc = ref.get().await()
            if (doc.exists()) {
                val current = doc.toObject(ScreenTimeSession::class.java) ?: ScreenTimeSession()
                val updatedSessions = current.sessions + SessionEntry(start, now, durationMinutes)
                ref.update(
                    mapOf(
                        "totalMinutes" to current.totalMinutes + durationMinutes,
                        "sessions" to updatedSessions
                    )
                ).await()
            } else {
                // Create new screen time record
                val session = ScreenTimeSession(
                    id = date,
                    userId = userId,
                    date = date,
                    totalMinutes = durationMinutes,
                    sessions = listOf(SessionEntry(start, now, durationMinutes))
                )
                ref.set(session).await()
            }
        } catch (e: Exception) {
            // Log save errors
            Log.e("ScreenTimeManager", "Failed to save session", e)
        }
    }

    // Realtime listener for today's usage
    fun getTodayUsageFlow(userId: String): Flow<ScreenTimeSession?> = callbackFlow {
        val date = todayDate()
        val listener = sessionRef(userId, date)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("ScreenTimeManager", "Error loading today's usage", error)
                    trySend(null)
                    return@addSnapshotListener
                }
                val session = snapshot?.toObject(ScreenTimeSession::class.java)
                trySend(session)
            }
        awaitClose { listener.remove() }
    }

    // Realtime listener for weekly usage data
    fun getWeeklyUsageFlow(userId: String): Flow<List<ScreenTimeSession>> = callbackFlow {
        // Realtime listener for weekly usage data
        val dates = (0..6).map { daysAgo ->
            val cal = java.util.Calendar.getInstance()
            cal.add(java.util.Calendar.DAY_OF_YEAR, -daysAgo)
            SimpleDateFormat("yyyy-MM-dd", Locale.US).format(cal.time)
        }

        // Listen to today's document for real-time updates
        val listener = firestore.collection("screenTime")
            .document(userId)
            .collection("daily")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                // Convert Firestore data into session objects
                val sessions = snapshot?.documents
                    ?.mapNotNull { it.toObject(ScreenTimeSession::class.java) }
                    ?.filter { it.date in dates }
                    ?.sortedByDescending { it.date }
                    ?: emptyList()
                trySend(sessions)
            }
        awaitClose { listener.remove() }
    }

    // Child requests more screen time
    suspend fun requestExtension(userId: String) {
        val date = todayDate()
        try {
            sessionRef(userId, date).update("extensionRequested", true).await()
        } catch (e: Exception) {
            Log.e("ScreenTimeManager", "Failed to request extension", e)
        }
    }


    // Parent grants additional screen time
    suspend fun grantExtension(childId: String, additionalMinutes: Int) {
        val date = todayDate()
        val ref = sessionRef(childId, date)
        try {
            val doc = ref.get().await()
            if (doc.exists()) {
                // Add bonus minutes to existing record
                val current = doc.getLong("bonusMinutes")?.toInt() ?: 0
                ref.update(
                    mapOf(
                        "extensionGranted" to true,
                        "bonusMinutes" to current + additionalMinutes
                    )
                ).await()
            }
        } catch (e: Exception) {
            Log.e("ScreenTimeManager", "Failed to grant extension", e)
        }
    }
}
