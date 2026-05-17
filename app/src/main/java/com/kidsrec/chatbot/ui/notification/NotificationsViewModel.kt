package com.kidsrec.chatbot.ui.notification

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.kidsrec.chatbot.data.model.UserNotification
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

// ViewModel responsible for loading and updating user notifications
@HiltViewModel
class NotificationsViewModel @Inject constructor(
    private val firestore: FirebaseFirestore
) : ViewModel() {

    // Stores the list of notifications displayed in the notification screen
    private val _uiState = MutableStateFlow<List<UserNotification>>(emptyList())
    val uiState: StateFlow<List<UserNotification>> = _uiState.asStateFlow()

    // Keeps track of the Firestore realtime listener so it can be removed when no longer needed
    private var listenerRegistration: com.google.firebase.firestore.ListenerRegistration? = null

    // Starts listening to the current user's notifications in realtime
    fun startListening(userId: String) {
        // Removes any existing listener to avoid duplicate listeners
        listenerRegistration?.remove()

        // Listens to the user's notification items, newest first
        listenerRegistration = firestore.collection("userNotifications")
            .document(userId)
            .collection("items")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(50)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("NotificationsVM", "Error listening for notifications", error)
                    return@addSnapshotListener
                }
                // Converts Firestore documents into UserNotification objects
                val notifications = snapshot?.documents?.map { doc ->
                    UserNotification(
                        id = doc.id,
                        title = doc.getString("title") ?: "",
                        body = doc.getString("body") ?: "",
                        type = doc.getString("type") ?: "",
                        read = doc.getBoolean("read") ?: false,
                        category = doc.getString("category") ?: "",
                        createdAt = doc.getLong("createdAt") ?: 0L
                    )
                } ?: emptyList()

                // Updates the UI state with the latest notification list
                _uiState.value = notifications
            }
    }

    // Marks a specific notification as read in Firestore
    fun markRead(userId: String, notificationId: String) {
        viewModelScope.launch {
            try {
                firestore.collection("userNotifications")
                    .document(userId)
                    .collection("items")
                    .document(notificationId)
                    .update("read", true)
                    .await()
            } catch (e: Exception) {
                Log.e("NotificationsVM", "Failed to mark notification as read", e)
            }
        }
    }

    // Cleans up the Firestore listener when the ViewModel is destroyed
    override fun onCleared() {
        super.onCleared()
        listenerRegistration?.remove()
    }
}
